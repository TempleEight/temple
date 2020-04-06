package temple.builder.project

import temple.ast.Metadata._
import temple.ast.{Metadata, StructBlock, Templefile}
import temple.builder._
import temple.detail.LanguageDetail
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.FileSystem._
import temple.generate.database.PreparedType.QuestionMarks
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

  def endpoints(service: StructBlock[_]): Set[CRUD] = {
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

    val usesAuth = templefile.services.exists {
      case (_, service) => service.lookupMetadata[ServiceAuth].nonEmpty
    }

    val databaseCreationScripts = templefile.services.map {
      case (name, service) =>
        val createStatements: Seq[Statement.Create] = DatabaseBuilder.createServiceTables(name, service)
        service.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase) match {
          case Database.Postgres =>
            implicit val context: PostgresContext = PostgresContext(QuestionMarks)
            val postgresStatements                = createStatements.map(PostgresGenerator.generate).mkString("\n\n")
            (File(s"${kebabCase(name)}-db", "init.sql"), postgresStatements)
        }
    }

    val dockerfiles = templefile.servicesWithPorts.map {
      case (name, service, port) =>
        val dockerfileRoot     = DockerfileBuilder.createServiceDockerfile(kebabCase(name), service, port.service)
        val dockerfileContents = DockerfileGenerator.generate(dockerfileRoot)
        (File(s"${kebabCase(name)}", "Dockerfile"), dockerfileContents)
    }

    val openAPIRoot = OpenAPIBuilder.createOpenAPI(templefile)
    val apiFiles    = OpenAPIGenerator.generate(openAPIRoot)

    val orchestrationRoot = OrchestrationBuilder.createServiceOrchestrationRoot(
      kebabCase(templefile.projectName),
      templefile.servicesWithPorts.map { case (name, block, ports) => (name, block, ports.service) }.toSeq,
    )
    val kubeFiles = KubernetesGenerator.generate(orchestrationRoot)

    // TODO: Get this from templefile and project settings
    val datasource: Datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    // TODO: Take all of this inside MetricsBuilder
    val metrics = templefile.services.map {
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
          templefile.servicesWithPorts.map {
            case (serviceName, _, ports) =>
              PrometheusJob(kebabCase(serviceName), s"${kebabCase(serviceName)}:${ports.metrics}")
          }.toSeq,
        ),
      )

    var serverFiles = templefile.servicesWithPorts.flatMap {
      case (name, service, port) =>
        val serviceRoot =
          ServerBuilder.buildServiceRoot(name, service, port.service, endpoints(service), detail, usesAuth)
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
