package temple.builder.project

import temple.ast.Metadata._
import temple.generate.database.PreparedType
import temple.generate.orchestration.ast.OrchestrationType.DbStorage

object ProjectConfig {

  case class DockerImage(image: String, version: String) {
    override def toString: String = image + ":" + version
  }

  val registryURL                      = "localhost:5000"
  val defaultLanguage: ServiceLanguage = ServiceLanguage.Go
  val defaultDatabase: Database        = Database.Postgres
  val defaultAuth: AuthMethod          = AuthMethod.Email
  val authPort: Int                    = 1024
  val authMetricPort: Int              = 1025
  val serviceStartPort: Int            = 1026

  def dockerImage(language: ServiceLanguage): DockerImage = language match {
    case ServiceLanguage.Go => DockerImage("golang", "1.13.7-alpine")
  }

  def dockerImage(database: Database): DockerImage = database match {
    case Database.Postgres => DockerImage("postgres", "12.1")
  }

  def preparedType(language: ServiceLanguage): PreparedType = language match {
    case ServiceLanguage.Go => PreparedType.DollarNumbers
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
