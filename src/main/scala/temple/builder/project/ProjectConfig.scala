package temple.builder.project

import temple.DSL.semantics.Metadata.{Database, ServiceLanguage}

object ProjectConfig {
  val defaultLanguage: ServiceLanguage = ServiceLanguage.Go
  val defaultDatabase: Database        = Database.Postgres
}
