package temple.generate.target.openapi

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import temple.generate.JsonEncodable
import temple.generate.target.openapi.HTTPVerb.{HTTPVerb, httpVerbKeyEncoder}
import temple.generate.target.openapi.OpenAPIFile._

case class OpenAPIFile(
  info: Info,
  paths: Map[String, Map[HTTPVerb, Path]] = Map.empty,
  components: Components = Components(),
) extends JsonEncodable {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "openapi"    -> "3.0.0".asJson,
    "info"       -> info.asJson,
    "paths"      -> paths.asJson,
    "components" -> components.asJson,
  )
}

object OpenAPIFile {
  case class Components(responses: Map[String, Response] = Map.empty)

  case class Info(title: String, version: String, description: Option[String])
}
