package temple.builder.project

import temple.DSL.semantics.Metadata.Database
import temple.DSL.semantics.Metadata.Database.Postgres
import temple.DSL.semantics.Templefile
import temple.builder.DatabaseBuilder
import temple.builder.project.FileType.SQL
import temple.builder.project.Project.File
import temple.generate.database.PreparedType.QuestionMarks
import temple.generate.database.{PostgresContext, PostgresGenerator}
import temple.generate.database.ast.Statement

object ProjectBuilder {

  /**
    * Converts a Templefile to an associated project, containing all generated code
    * @param templefile The semantically correct Templefile
    * @return the associated generated project
    */
  def build(templefile: Templefile): Project = {
    val databaseCreationScripts = templefile.services.map {
      case (name, service) =>
        val queries: Seq[Statement.Create] = DatabaseBuilder.createServiceTables(name, service)
        service.lookupMetadata[Database].getOrElse(Postgres) match {
          case Postgres =>
            implicit val context: PostgresContext = PostgresContext(QuestionMarks)
            val postgresStatements                = queries.map(PostgresGenerator.generate).mkString("\n")
            (File(s"$name-db", "init", SQL), postgresStatements)
        }
    }

    Project(databaseCreationScripts)
  }
}
