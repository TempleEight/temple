package temple.generate.server.config.ast

import io.circe.Json
import temple.generate.JsonEncodable

trait DatabaseConfig extends JsonEncodable.Object

case class PostgresConfig(host: String) extends DatabaseConfig {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "user"    ~> "postgres",
    "dbName"  ~> "postgres",
    "host"    ~> host,
    "sslMode" ~> "disable",
  )
}
