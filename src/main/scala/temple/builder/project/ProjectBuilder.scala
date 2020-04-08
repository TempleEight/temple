package temple.builder.project

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
import temple.generate.orchestration.kube.KubernetesGenerator
import temple.generate.metrics.grafana.ast.Datasource
import temple.generate.metrics.grafana.{GrafanaDashboardConfigGenerator, GrafanaDashboardGenerator, GrafanaDatasourceConfigGenerator}
import temple.generate.metrics.prometheus.PrometheusConfigGenerator
import temple.generate.metrics.prometheus.ast.PrometheusJob
import temple.generate.server.config.ServerConfigGenerator
import temple.generate.server.go.auth.GoAuthServiceGenerator
import temple.generate.server.go.service.GoServiceGenerator
import temple.generate.target.openapi.OpenAPIGenerator
import temple.utils.StringUtils._

object ProjectBuilder {

  def endpoints(service: AttributeBlock[_]): Set[CRUD] = {
    val endpoints: Set[CRUD] = service
      .lookupMetadata[Metadata.Omit]
      .map(_.endpoints)
      .getOrElse(Set.empty)
      .foldLeft(Set[CRUD](Create, Read, Update, Delete)) {
        case (set, endpoint) =>
          endpoint match {
            case Endpoint.Create => set - CRUD.Create
            case Endpoint.Read   => set - CRUD.Read
            case Endpoint.Update => set - CRUD.Update
            case Endpoint.Delete => set - CRUD.Delete
          }
      }
    // Add read all endpoint if defined
    service.lookupMetadata[Metadata.ServiceEnumerable].fold(endpoints)(_ => endpoints + List)
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

  private def buildDockerfiles(templefile: Templefile): Files =
    templefile.allServicesWithPorts.map {
      case (name, service, port) =>
        val dockerfileRoot     = DockerfileBuilder.createServiceDockerfile(kebabCase(name), service, port.service)
        val dockerfileContents = DockerfileGenerator.generate(dockerfileRoot)
        (File(s"${kebabCase(name)}", "Dockerfile"), dockerfileContents)
    }.toMap

  private def buildOpenAPI(templefile: Templefile): Files = {
    val openAPIRoot = OpenAPIBuilder.createOpenAPI(templefile)
    OpenAPIGenerator.generate(openAPIRoot)
  }

  private def buildOrchestration(templefile: Templefile): Files = {
    val orchestrationRoot = OrchestrationBuilder.createServiceOrchestrationRoot(templefile)
    KubernetesGenerator.generate(templefile.projectName, orchestrationRoot)
  }

  private def buildMetrics(templefile: Templefile): Files = {
    val metric = templefile.lookupMetadata[Metrics].getOrElse {
      // If no explicit metrics tag is given, no files are to be generated, therefore can early return
      return Map.empty
    }

    val datasource = metric match {
      case Metrics.Prometheus => Datasource.Prometheus("Prometheus", "http://prom:9090")
    }

    val dashboardJSONs = templefile.allServices.map {
      case (name, service) =>
        // Create row for every endpoint in the service
        val serviceRows = MetricsBuilder.createDashboardRows(name, datasource, endpoints(service))
        val structRows = service.structs.flatMap {
          case (structName, struct) =>
            MetricsBuilder.createDashboardRows(name, datasource, endpoints(struct), Some(structName))
        }
        val grafanaDashboard = GrafanaDashboardGenerator.generate(kebabCase(name), name, serviceRows ++ structRows)
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
      case (_, service) => service.lookupMetadata[ServiceAuth].nonEmpty
    }

    var serverFiles = templefile.providedServicesWithPorts.flatMap {
      case (name, service, port) =>
        val serviceRoot =
          ServerBuilder.buildServiceRoot(name, service, port.service, endpoints(service), detail, usesAuth)
        val serverFiles = service.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage) match {
          case ServiceLanguage.Go => GoServiceGenerator.generate(serviceRoot)
        }

        val serviceComms = serviceRoot.comms.map { service =>
          val (_, _, ports) = templefile.providedServicesWithPorts.find { _._1 == service }.get
          service -> s"http://${kebabCase(service)}:${ports.service}"
        }.toMap

        val configFileContents =
          ServerConfigGenerator.generate(serviceRoot.kebabName, serviceRoot.datastore, serviceComms, port)

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
            )
          serverFiles = serverFiles ++ (GoAuthServiceGenerator.generate(authRoot) + (File("auth", "config.json") -> configFileContents))
      }
    }

    serverFiles.toMap
  }

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
      buildMetrics(templefile),
    )
}
