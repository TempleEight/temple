package temple.generate.target.openapi.ast

import io.circe.Json
import temple.generate.JsonEncodable

/** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#response-object */
case class ResponseObject(
  description: String = "",
  content: Option[Map[String, MediaTypeObject]] = None,
  headers: Option[Map[String, HeaderObject]] = None,
) extends JsonEncodable.Partial
    with Response {

  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
    "description" ~~> Option.when(description.nonEmpty)(description),
    "headers"     ~~> headers,
    "content"     ~~> content,
  )
}
