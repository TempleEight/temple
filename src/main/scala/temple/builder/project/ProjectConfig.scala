package temple.builder.project

import temple.DSL.semantics.Metadata.{Database, ServiceLanguage}

object ProjectConfig {
  case class DockerImage(image: String, version: String)
  val defaultLanguage: ServiceLanguage = ServiceLanguage.Go
  val defaultDatabase: Database        = Database.Postgres

  def dockerImage(language: ServiceLanguage): DockerImage = language match {
    case ServiceLanguage.Go => DockerImage("golang", "1.13.7-alpine")
  }
}
