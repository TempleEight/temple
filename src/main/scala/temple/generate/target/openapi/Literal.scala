package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable

/** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#requestBodyObject */
case class Literal(
  content: Map[String, MediaTypeObject],
  description: String = "",
  required: Option[Boolean] = None,
) extends JsonEncodable.Partial
    with RequestBody
    with Response {

  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
    "description" -> Option.when(description.nonEmpty)(description.asJson),
    "required"    -> required.map(_.asJson),
    "content"     -> Some(content.asJson),
  )
}
