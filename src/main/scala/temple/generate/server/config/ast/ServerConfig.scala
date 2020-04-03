package temple.generate.server.config.ast

import io.circe.Json
import temple.ast.Templefile.Ports
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap

case class ServerConfig(databaseConfig: DatabaseConfig, services: Map[String, String], ports: Ports)
    extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    databaseConfig.jsonEntryIterator.iterator.toSeq ++
    Seq(
      "services" ~> services,
      "ports" ~> ListMap(
        "service"    ~> ports.service,
        "prometheus" ~> ports.metrics,
      ),
    )
}
