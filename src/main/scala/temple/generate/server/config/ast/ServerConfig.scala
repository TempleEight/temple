package temple.generate.server.config.ast

import io.circe.Json
import temple.ast.Metadata.Metrics
import temple.ast.Templefile.Ports
import temple.generate.JsonEncodable

case class ServerConfig(
  databaseConfig: DatabaseConfig,
  services: Map[String, String],
  ports: Ports,
  metrics: Option[Metrics],
) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    databaseConfig.jsonEntryIterator.iterator.toSeq ++
    Seq(
      "services" ~> services,
      "ports" ~> (Map(
        "service" ~> ports.service,
      ) ++ metrics.map {
        case Metrics.Prometheus => "prometheus" ~> ports.metrics
      }),
    )
}
