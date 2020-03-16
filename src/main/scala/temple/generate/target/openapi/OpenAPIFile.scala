package temple.generate.target.openapi

import io.circe.Json
import io.circe.generic.auto._
import temple.generate.JsonEncodable
import temple.generate.target.openapi.HTTPVerb.httpVerbKeyEncoder
import temple.generate.target.openapi.OpenAPIFile._

import scala.Option.when

case class OpenAPIFile(
  info: Info,
  paths: Map[String, Map[HTTPVerb, Handler]] = Map.empty,
  components: Components = Components(),
) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "openapi"    ~> "3.0.0",
    "info"       ~> info,
    "paths"      ~> paths,
    "components" ~> components,
  )
}

object OpenAPIFile {
  case class Components(responses: Map[String, Response] = Map.empty)

  case class Info(title: String, version: String, description: String) extends JsonEncodable.Partial {

    override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
      "title"       ~~> Some(title),
      "version"     ~~> when(version.nonEmpty) { version },
      "description" ~~> when(description.nonEmpty) { description },
    )
  }
}
