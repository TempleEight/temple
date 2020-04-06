package temple.builder.project

import temple.ast.Metadata._
import temple.generate.kube.ast.OrchestrationType.DbStorage

object ProjectConfig {

  case class DockerImage(image: String, version: String) {
    override def toString: String = image + ":" + version
  }

  val defaultLanguage: ServiceLanguage = ServiceLanguage.Go
  val defaultDatabase: Database        = Database.Postgres
  val defaultAuth: ServiceAuth         = ServiceAuth.Email
  val authPort: Int                    = 1024
  val authMetricPort: Int              = 1025
  val serviceStartPort: Int            = 1026

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

  def getDefaultReadable(projectHasAuthBlock: Boolean): Readable =
    if (projectHasAuthBlock) Readable.This else Readable.All

  def getDefaultWritable(projectHasAuthBlock: Boolean): Writable =
    if (projectHasAuthBlock) Writable.This else Writable.All
}
