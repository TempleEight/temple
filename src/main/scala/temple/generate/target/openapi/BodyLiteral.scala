package temple.generate.target.openapi

import io.circe.Json
import temple.generate.JsonEncodable

/** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#requestBodyObject */
case class BodyLiteral(
  content: Map[String, MediaTypeObject],
  description: String = "",
  required: Option[Boolean] = None,
) extends JsonEncodable.Partial
    with RequestBody
    with Response {

  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
    "description" ~~> Option.when(description.nonEmpty)(description),
    "required"    ~~> required,
    "content"     ~~> Some(content),
  )
}
