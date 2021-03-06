package temple.builder.project

import temple.ast.AbstractServiceBlock.AuthServiceBlock
import temple.ast.Metadata.Provider.DockerCompose
import temple.ast.Metadata._
import temple.ast.Templefile.Ports
import temple.ast._
import temple.builder._
import temple.detail.LanguageDetail
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.FileSystem._
import temple.generate.database.ast.Statement
import temple.generate.database.{PostgresContext, PostgresGenerator}
import temple.generate.docker.DockerfileGenerator
import temple.generate.docs.DocumentationGenerator
import temple.generate.metrics.grafana.ast.{Datasource, Row}
import temple.generate.metrics.grafana.{GrafanaDashboardConfigGenerator, GrafanaDashboardGenerator, GrafanaDatasourceConfigGenerator}
import temple.generate.metrics.prometheus.PrometheusConfigGenerator
import temple.generate.metrics.prometheus.ast.PrometheusJob
import temple.generate.orchestration.dockercompose.DockerComposeGenerator
import temple.generate.orchestration.kube.KubernetesGenerator
import temple.generate.server.config.ServerConfigGenerator
import temple.generate.server.go.auth.GoAuthServiceGenerator
import temple.generate.server.go.service.GoServiceGenerator
import temple.generate.target.openapi.OpenAPIGenerator
import temple.utils.FileUtils
import temple.utils.StringUtils._

import scala.Option.when
import scala.collection.immutable.SortedSet

object ProjectBuilder {

  def endpoints(block: AttributeBlock[_]): SortedSet[CRUD] = {
    val omitted = block.lookupLocalMetadata[Metadata.Omit].iterator.flatMap(_.endpoints).map {
      case Endpoint.Create => CRUD.Create
      case Endpoint.Read   => CRUD.Read
      case Endpoint.Update => CRUD.Update
      case Endpoint.Delete => CRUD.Delete
    }
    (SortedSet(Create, Read, Update, Delete) -- omitted
    ++ when(block hasMetadata Metadata.ServiceEnumerable) { List }
    ++ when(block hasMetadata Metadata.ServiceAuth) { Identify })
  }

  private def buildDatabaseCreationScripts(templefile: Templefile): Files =
    templefile.allServices.map {
      case (name, service) =>
        val createStatements: Seq[Statement.Create] = DatabaseBuilder.createServiceTables(name, service)
        service.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase) match {
          case Database.Postgres =>
            implicit val dbContext: PostgresContext = PostgresContext(
              ProjectConfig
                .preparedType(service.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)),
            )
            val postgresStatements = createStatements.map(PostgresGenerator.generate).mkString("", "\n\n", "\n")
            (File(s"${kebabCase(name)}-db", "init.sql"), postgresStatements)
        }
    }

  private def buildDockerfiles(templefile: Templefile): Files = {
    val provider = templefile.lookupMetadata[Provider]
    val dockerfiles = templefile.allServicesWithPorts.map {
      case (name, service, port) =>
        val dockerfileRoot     = DockerfileBuilder.createServiceDockerfile(kebabCase(name), service, port.service, provider)
        val dockerfileContents = DockerfileGenerator.generate(dockerfileRoot)
        (File(s"${kebabCase(name)}", "Dockerfile"), dockerfileContents)
    }.toMap
    dockerfiles ++ when(templefile.usesAuth && provider.contains(DockerCompose)) {
      File(s"auth", "wait-for-kong.sh") -> FileUtils.readResources("shell/wait-for-kong.sh")
    }
  }

  private def buildOpenAPI(templefile: Templefile): Files = {
    val openAPIRoot = OpenAPIBuilder.createOpenAPI(templefile)
    OpenAPIGenerator.generate(openAPIRoot)
  }

  private def buildOrchestration(templefile: Templefile): Files = {
    val orchestrationRoot = OrchestrationBuilder.createServiceOrchestrationRoot(templefile)
    templefile.lookupMetadata[Provider].getOrElse(return Map()) match {
      case Provider.Kubernetes =>
        KubernetesGenerator.generate(templefile.projectName, orchestrationRoot)
      case Provider.DockerCompose => DockerComposeGenerator.generate(templefile.projectName, orchestrationRoot)
    }
  }

  private def buildMetrics(templefile: Templefile): Files = {
    val metric = templefile.lookupMetadata[Metrics].getOrElse {
      // If no explicit metrics tag is given, no files are to be generated, therefore can early return
      return Map.empty
    }

    val datasource = metric match {
      case Metrics.Prometheus => Datasource.Prometheus("Prometheus", "http://prometheus:9090")
    }

    val dashboardJSONs = templefile.allServices
      .map {
        case (name, AuthServiceBlock) => (name, MetricsBuilder.createAuthDashboardRows(name, datasource))
        case (name, service)          =>
          // Create row for every endpoint in the service
          val serviceRows = MetricsBuilder.createDashboardRows(name, datasource, endpoints(service))
          val structRows = service.structs.foldLeft(Seq[Row]()) {
            case (rows, (structName, struct)) =>
              rows ++ MetricsBuilder.createDashboardRows(
                name,
                datasource,
                endpoints(struct),
                Some(structName),
                serviceRows.length + rows.length,
              )
          }
          (name, serviceRows ++ structRows)
      }
      .map {
        case (name, rows) =>
          val grafanaDashboard = GrafanaDashboardGenerator.generate(kebabCase(name), name, rows)
          File(s"grafana/provisioning/dashboards", s"${kebabCase(name)}.json") -> grafanaDashboard
      }
    val dashboardYML  = GrafanaDashboardConfigGenerator.generate(datasource)
    val datasourceYML = GrafanaDatasourceConfigGenerator.generate(datasource)

    // Create a job for every service
    val prometheusJobs = templefile.allServicesWithPorts.map {
      case (serviceName, _, ports) =>
        PrometheusJob(kebabCase(serviceName), s"${kebabCase(serviceName)}:${ports.metrics}")
    }.toSeq
    val prometheusYML = PrometheusConfigGenerator.generate(prometheusJobs)

    dashboardJSONs ++ Map(
      File(s"grafana/provisioning/dashboards", "dashboards.yml")  -> dashboardYML,
      File(s"grafana/provisioning/datasources", "datasource.yml") -> datasourceYML,
      File(s"prometheus", "prometheus.yml")                       -> prometheusYML,
    )
  }

  private def buildServerFiles(templefile: Templefile, detail: LanguageDetail): Files = {
    // Whether or not to generate an auth service - based on whether any service has #auth
    val usesAuth = templefile.services.exists {
      case (_, service) => service.lookupLocalMetadata[ServiceAuth].nonEmpty
    }

    val metrics = templefile.lookupMetadata[Metrics]

    var serverFiles = templefile.providedServicesWithPorts.flatMap {
      case (name, service, port) =>
        val serviceRoot =
          ServerBuilder.buildServiceRoot(name, service, port.service, detail, usesAuth)
        val serverFiles = service.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage) match {
          case ServiceLanguage.Go => GoServiceGenerator.generate(serviceRoot)
        }

        val serviceComms = serviceRoot.comms.map { service =>
          val (_, _, ports) = templefile.providedServicesWithPorts.find { _._1 == service.name }.get
          service.kebabName -> s"http://${service.kebabName}:${ports.service}/${service.kebabName}"
        }.toMap

        val configFileContents =
          ServerConfigGenerator.generate(serviceRoot.kebabName, serviceRoot.datastore, serviceComms, port, metrics)

        serverFiles + (File(serviceRoot.kebabName, "config.json") -> configFileContents)
    }

    if (usesAuth) {
      templefile.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage) match {
        case ServiceLanguage.Go =>
          val authRoot = ServerBuilder.buildAuthRoot(templefile, detail, ProjectConfig.authPort)
          // TODO: Use authRoot.datastore when it's defined
          val configFileContents =
            ServerConfigGenerator.generate(
              "auth",
              Database.Postgres,
              Map("kong-admin" -> "http://kong:8001"),
              Ports(ProjectConfig.authPort, ProjectConfig.authMetricPort),
              metrics,
            )
          serverFiles = serverFiles ++ (GoAuthServiceGenerator.generate(authRoot) + (File("auth", "config.json") -> configFileContents))
      }
    }

    serverFiles.toMap
  }

  private def buildDocumentation(templefile: Templefile): Files =
    DocumentationGenerator.generate(templefile)

  /**
    * Converts a Templefile to an associated project, containing all generated code
    *
    * @param templefile The semantically correct Templefile
    * @return the associated generated project
    */
  def build(templefile: Templefile, detail: LanguageDetail): Project =
    Project(
      buildDatabaseCreationScripts(templefile) ++
      buildDockerfiles(templefile) ++
      buildOrchestration(templefile) ++
      buildOpenAPI(templefile) ++
      buildServerFiles(templefile, detail) ++
      buildMetrics(templefile) ++
      buildDocumentation(templefile),
    )
}
