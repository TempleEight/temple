package temple.generate.orchestration.dockercompose.ast

import io.circe.Json
import temple.generate.JsonEncodable

case class DockerComposeRoot(services: Map[String, Service], networks: Map[String, Map[String, String]])
    extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "version"  ~> '3',
    "services" ~> services,
    "networks" ~> networks,
  )
}
