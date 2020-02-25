package temple.builder.project

import temple.DSL.semantics.Metadata.Database
import temple.DSL.semantics.Metadata.Database.Postgres
import temple.DSL.semantics.Templefile
import temple.builder.{DatabaseBuilder, DockerfileBuilder}
import temple.utils.FileUtils.File
import temple.generate.database.PreparedType.QuestionMarks
import temple.generate.database.ast.Statement
import temple.generate.database.{PostgresContext, PostgresGenerator}
import temple.generate.docker.DockerfileGenerator

object ProjectBuilder {

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
          case Postgres =>
            implicit val context: PostgresContext = PostgresContext(QuestionMarks)
            val postgresStatements                = createStatements.map(PostgresGenerator.generate).mkString("\n\n")
            (File(s"${name.toLowerCase}-db", "init.sql"), postgresStatements)
        }
    }

    val dockerfiles = templefile.services.zip(1024 until Int.MaxValue).map {
      case ((name, service), port) =>
        val dockerfileRoot     = DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, port)
        val dockerfileContents = DockerfileGenerator.generate(dockerfileRoot)
        (File(s"${name.toLowerCase}", "Dockerfile"), dockerfileContents)
    }

    Project(databaseCreationScripts ++ dockerfiles)
  }
}
