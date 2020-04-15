package temple.generate.server.config

import io.circe.syntax._
import temple.ast.Metadata.{Database, Metrics}
import temple.ast.Templefile.Ports
import temple.generate.server.config.ast.{PostgresConfig, ServerConfig}

object ServerConfigGenerator {

  def generate(
    serviceName: String,
    database: Database,
    services: Map[String, String],
    ports: Ports,
    metrics: Option[Metrics],
  ): String = {
    val databaseConfig = database match {
      case Database.Postgres => PostgresConfig(serviceName + "-db")
    }
    ServerConfig(databaseConfig, services, ports, metrics).asJson.toString + "\n"
  }
}
