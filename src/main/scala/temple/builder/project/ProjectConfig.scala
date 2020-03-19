package temple.builder.project

import temple.generate.kube.ast.OrchestrationType.DbStorage
import temple.ast.Metadata._


object ProjectConfig {

  case class DockerImage(image: String, version: String) {
    override def toString: String = image + ":" + version
  }

  val defaultLanguage: ServiceLanguage = ServiceLanguage.Go
  val defaultDatabase: Database        = Database.Postgres

  def dockerImage(language: ServiceLanguage): DockerImage = language match {
    case ServiceLanguage.Go => DockerImage("golang", "1.13.7-alpine")
  }

  def dockerImage(database: Database): DockerImage = database match {
    case Database.Postgres => DockerImage("postgres", "12.1")
  }

  def databaseStorage(database: Database, serviceName: String): DbStorage = database match {
    case Database.Postgres =>
      DbStorage(
        dataMount = "/var/lib/postgresql/data",
        initMount = "/docker-entrypoint-initdb.d/init.sql",
        initFile = "init.sql",
        hostPath = s"/data/$serviceName-db",
      )
  }
}
