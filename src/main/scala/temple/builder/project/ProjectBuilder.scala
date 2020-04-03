package temple.builder.project

import temple.ast.Metadata._
import temple.ast.{Annotation, Attribute, AttributeType, Metadata, ServiceBlock, TempleBlock, Templefile}
import temple.builder._
import temple.detail.LanguageDetail
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.FileSystem._
import temple.generate.database.PreparedType.QuestionMarks
import temple.generate.database.ast.Statement
import temple.generate.database.{PostgresContext, PostgresGenerator, PreparedType}
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

    val usesAuth = templefile.services.exists {
      case (_, service) => service.lookupMetadata[ServiceAuth].nonEmpty
    }

    // Auth service block only used if usesAuth == true
    val authServiceBlock: ServiceBlock = ServiceBlock(
      attributes = Map(
        "id"       -> Attribute(AttributeType.UUIDType, valueAnnotations = Set(Annotation.Unique)),
        "email"    -> Attribute(AttributeType.StringType(), valueAnnotations = Set(Annotation.Unique)),
        "password" -> Attribute(AttributeType.StringType()),
      ),
    )
    authServiceBlock.setParent(templefile)

    val databaseCreationScripts = templefile.services.map {
        case (name, service) =>
          val createStatements: Seq[Statement.Create] = DatabaseBuilder.createServiceTables(name, service)
          service.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase) match {
            case Database.Postgres =>
              implicit val context: PostgresContext = PostgresContext(QuestionMarks)
              val postgresStatements                = createStatements.map(PostgresGenerator.generate).mkString("\n\n")
              (File(s"${name.toLowerCase}-db", "init.sql"), postgresStatements)
          }
      } ++ Option.when(usesAuth) {
        templefile.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase) match {
          case Database.Postgres =>
            implicit val postgresContext: PostgresContext = PostgresContext(PreparedType.DollarNumbers)
            val dbAuthFile = DatabaseBuilder
              .createServiceTables("auth", authServiceBlock)
              .map(PostgresGenerator.generate)
              .mkString("\n\n")
            File("auth-db", "init.sql") -> dbAuthFile
        }
      }

    val dockerfiles = templefile.servicesWithPorts.map {
        case (name, service, port) =>
          val dockerfileRoot     = DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, port.service)
          val dockerfileContents = DockerfileGenerator.generate(dockerfileRoot)
          (File(s"${name.toLowerCase}", "Dockerfile"), dockerfileContents)
      } ++ Option.when(usesAuth) {
        val authDockerFile = DockerfileGenerator.generate(
          DockerfileBuilder.createServiceDockerfile("auth", authServiceBlock, ProjectConfig.authPort),
        )
        File("auth", "Dockerfile") -> authDockerFile
      }

    val openAPIRoot = OpenAPIBuilder.createOpenAPI(templefile)
    val apiFiles    = OpenAPIGenerator.generate(openAPIRoot)

    val orchestrationRoot = OrchestrationBuilder.createServiceOrchestrationRoot(
      StringUtils.kebabCase(templefile.projectName),
      templefile.servicesWithPorts.map { case (name, block, ports) => (name, block, ports.service) }.toSeq
      ++ Option.when(usesAuth)("auth", authServiceBlock, ProjectConfig.authPort),
    )
    val kubeFiles = KubernetesGenerator.generate(orchestrationRoot)

    // TODO: Get this from templefile and project settings
    val datasource: Datasource = Datasource.Prometheus("Prometheus", "http://prom:9090")
    // TODO: Take all of this inside MetricsBuilder
    val metrics = templefile.services.map {
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
          templefile.servicesWithPorts.map {
            case (serviceName, _, ports) =>
              PrometheusJob(serviceName.toLowerCase, s"${serviceName.toLowerCase}:${ports.metrics}")
          }.toSeq,
        ),
      ) ++ Option.when(usesAuth) {
        val rows      = MetricsBuilder.createDashboardRows("auth", datasource, endpoints(authServiceBlock))
        val dashboard = GrafanaDashboardGenerator.generate("auth", "Auth", rows)
        File("grafana/provisioning/dashboards", "auth.json") -> dashboard
      }

    var serverFiles = templefile.servicesWithPorts.flatMap {
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
