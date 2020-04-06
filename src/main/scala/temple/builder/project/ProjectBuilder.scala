package temple.builder.project

import temple.ast.Metadata._
import temple.ast._
import temple.builder._
import temple.detail.LanguageDetail
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.FileSystem._
import temple.generate.database.PreparedType.DollarNumbers
import temple.generate.database.ast.Statement
import temple.generate.database.{PostgresContext, PostgresGenerator}
import temple.generate.docker.DockerfileGenerator
import temple.generate.kube.KubernetesGenerator
import temple.generate.metrics.grafana.ast.Datasource
import temple.generate.metrics.grafana.{GrafanaDashboardConfigGenerator, GrafanaDashboardGenerator, GrafanaDatasourceConfigGenerator}
import temple.generate.metrics.prometheus.PrometheusConfigGenerator
import temple.generate.metrics.prometheus.ast.PrometheusJob
import temple.generate.server.go.auth.GoAuthServiceGenerator
import temple.generate.server.go.service.GoServiceGenerator
import temple.generate.target.openapi.OpenAPIGenerator
import temple.utils.StringUtils._

object ProjectBuilder {

  def endpoints(service: TempleBlock[ServiceOrStructMetadata]): Set[CRUD] = {
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
            implicit val dbContext: PostgresContext = PostgresContext(ProjectConfig.defaultPreparedType)
            val postgresStatements                  = createStatements.map(PostgresGenerator.generate).mkString("\n\n")
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
    KubernetesGenerator.generate(orchestrationRoot)
  }

  private def buildMetrics(templefile: Templefile): Files = {
    // TODO: Get this from templefile and project settings
    val datasource: Datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    // TODO: Take all of this inside MetricsBuilder
    templefile.allServices.map {
      case (name, service) =>
        val rows             = MetricsBuilder.createDashboardRows(name, datasource, endpoints(service))
        val grafanaDashboard = GrafanaDashboardGenerator.generate(kebabCase(name), name, rows)
        File(s"grafana/provisioning/dashboards", s"${kebabCase(name)}.json") -> grafanaDashboard
    } ++ Map(
      File(s"grafana/provisioning/dashboards", "dashboards.yml") ->
      GrafanaDashboardConfigGenerator.generate(datasource),
      File(s"grafana/provisioning/datasources", "datasource.yml") ->
      GrafanaDatasourceConfigGenerator.generate(datasource),
      File(s"prometheus", "prometheus.yml") ->
      PrometheusConfigGenerator.generate(
        templefile.allServicesWithPorts.map {
          case (serviceName, _, ports) =>
            PrometheusJob(kebabCase(serviceName), s"${kebabCase(serviceName)}:${ports.metrics}")
        }.toSeq,
      ),
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
        service.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage) match {
          case ServiceLanguage.Go => GoServiceGenerator.generate(serviceRoot)
        }
    }

    if (usesAuth) {
      templefile.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage) match {
        case ServiceLanguage.Go =>
          serverFiles = serverFiles ++ GoAuthServiceGenerator.generate(
              ServerBuilder.buildAuthRoot(templefile, detail, ProjectConfig.authPort),
            )
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
