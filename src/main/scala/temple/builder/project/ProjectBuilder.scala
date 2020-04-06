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
import temple.utils.StringUtils

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

  /**
    * Converts a Templefile to an associated project, containing all generated code
    *
    * @param templefile The semantically correct Templefile
    * @return the associated generated project
    */
  def build(templefile: Templefile, detail: LanguageDetail): Project = {

    // Whether or not to generate an auth service - based on whether any service has #auth
    val usesAuth = templefile.services.exists {
      case (_, service) => service.lookupMetadata[ServiceAuth].nonEmpty
    }

    implicit val dbContext: PostgresContext = PostgresContext(DollarNumbers)

    val databaseCreationScripts = templefile.allServices.map {
      case (name, service) =>
        val createStatements: Seq[Statement.Create] = DatabaseBuilder.createServiceTables(name, service)
        service.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase) match {
          case Database.Postgres =>
            val postgresStatements = createStatements.map(PostgresGenerator.generate).mkString("\n\n")
            (File(s"${name.toLowerCase}-db", "init.sql"), postgresStatements)
        }
    }

    val dockerfiles = templefile.allServicesWithPorts.map {
      case (name, service, port) =>
        val dockerfileRoot     = DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, port.service)
        val dockerfileContents = DockerfileGenerator.generate(dockerfileRoot)
        (File(s"${name.toLowerCase}", "Dockerfile"), dockerfileContents)
    }

    val openAPIRoot = OpenAPIBuilder.createOpenAPI(templefile)
    val apiFiles    = OpenAPIGenerator.generate(openAPIRoot)

    val orchestrationRoot = OrchestrationBuilder.createServiceOrchestrationRoot(
      StringUtils.kebabCase(templefile.projectName),
      templefile.allServicesWithPorts.map { case (name, block, ports) => (name, block, ports.service) }.toSeq,
    )
    val kubeFiles = KubernetesGenerator.generate(orchestrationRoot)

    // TODO: Get this from templefile and project settings
    val datasource: Datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    // TODO: Take all of this inside MetricsBuilder
    val metrics = templefile.allServices.map {
        case (name, service) =>
          val rows             = MetricsBuilder.createDashboardRows(name, datasource, endpoints(service))
          val grafanaDashboard = GrafanaDashboardGenerator.generate(name.toLowerCase, name, rows)
          (File(s"grafana/provisioning/dashboards", s"${name.toLowerCase}.json"), grafanaDashboard)
      } ++ Map(
        File(s"grafana/provisioning/dashboards", "dashboards.yml") ->
        GrafanaDashboardConfigGenerator.generate(datasource),
        File(s"grafana/provisioning/datasources", "datasource.yml") ->
        GrafanaDatasourceConfigGenerator.generate(datasource),
        File(s"prometheus", "prometheus.yml") ->
        PrometheusConfigGenerator.generate(
          templefile.allServicesWithPorts.map {
            case (serviceName, _, ports) =>
              PrometheusJob(serviceName.toLowerCase, s"${serviceName.toLowerCase}:${ports.metrics}")
          }.toSeq,
        ),
      )

    var serverFiles = templefile.providedServicesWithPorts.flatMap {
      case (name, service, port) =>
        val serviceRoot =
          ServerBuilder.buildServiceRoot(name.toLowerCase, service, port.service, endpoints(service), detail)
        service.lookupMetadata[ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage) match {
          case ServiceLanguage.Go =>
            GoServiceGenerator.generate(serviceRoot)
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

    Project(
      databaseCreationScripts ++
      dockerfiles ++
      kubeFiles ++
      apiFiles ++
      serverFiles ++
      metrics,
    )
  }
}
