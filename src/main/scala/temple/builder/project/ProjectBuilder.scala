package temple.builder.project

import temple.DSL.semantics.Metadata.Database
import temple.DSL.semantics.Metadata.Database.Postgres
import temple.DSL.semantics.Templefile
import temple.builder.DatabaseBuilder
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
        val generatedScripts: Seq[String] = service.lookupMetadata[Database] match {
          case Some(Postgres) =>
            implicit val context: PostgresContext = PostgresContext(QuestionMarks)
            queries.map(PostgresGenerator.generate)

          // If no database is defined, default to Postgres
          case None =>
            implicit val context: PostgresContext = PostgresContext(QuestionMarks)
            queries.map(PostgresGenerator.generate)
        }
        (name, generatedScripts.mkString("\n"))
    }

    Project(databaseCreationScripts)
  }
}
