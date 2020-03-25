package temple.builder.project

import temple.ast.Metadata.{Database, Endpoint}
import temple.ast.{Metadata, ServiceBlock, Templefile}
import temple.builder.{DatabaseBuilder, DockerfileBuilder, MetricsBuilder, OrchestrationBuilder}
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.FileSystem._
import temple.generate.database.PreparedType.QuestionMarks
import temple.generate.database.ast.Statement
import temple.generate.database.{PostgresContext, PostgresGenerator}
import temple.generate.docker.DockerfileGenerator
import temple.generate.kube.KubernetesGenerator
import temple.generate.metrics.grafana.{GrafanaDashboardConfigGenerator, GrafanaDashboardGenerator}
import temple.utils.StringUtils

object ProjectBuilder {

  private def endpoints(service: ServiceBlock): Set[CRUD] = {
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
    * @param templefile The semantically correct Templefile
    * @return the associated generated project
    */
  def build(templefile: Templefile): Project = {
    val databaseCreationScripts = templefile.services.map {
      case (name, service) =>
        val createStatements: Seq[Statement.Create] = DatabaseBuilder.createServiceTables(name, service)
        service.lookupMetadata[Database].getOrElse(ProjectConfig.defaultDatabase) match {
          case Database.Postgres =>
            implicit val context: PostgresContext = PostgresContext(QuestionMarks)
            val postgresStatements                = createStatements.map(PostgresGenerator.generate).mkString("\n\n")
            (File(s"${name.toLowerCase}-db", "init.sql"), postgresStatements)
        }
    }

    val dockerfiles = templefile.servicesWithPorts.map {
      case (name, service, port) =>
        val dockerfileRoot     = DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, port)
        val dockerfileContents = DockerfileGenerator.generate(dockerfileRoot)
        (File(s"${name.toLowerCase}", "Dockerfile"), dockerfileContents)
    }

    val orchestrationRoot = OrchestrationBuilder.createServiceOrchestrationRoot(
      StringUtils.kebabCase(templefile.projectName),
      templefile.servicesWithPorts.toSeq,
    )
    val kubeFiles = KubernetesGenerator.generate(orchestrationRoot)

    val metrics = templefile.services.map {
        case (name, service) =>
          val rows             = MetricsBuilder.createDashboardRows(name, endpoints(service))
          val grafanaDashboard = GrafanaDashboardGenerator.generate(name.toLowerCase, name, rows)
          (File(s"grafana/provisioning/dashboards", s"${name.toLowerCase}.json"), grafanaDashboard)
      } + (
        File(s"grafana/provisioning/dashboards", "dashboards.yml") ->
        GrafanaDashboardConfigGenerator.generate("Prometheus"),
      )

    Project(databaseCreationScripts ++ dockerfiles ++ kubeFiles ++ metrics)
  }
}
