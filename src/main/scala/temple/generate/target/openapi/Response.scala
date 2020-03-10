package temple.generate.target.openapi

import io.circe.syntax._
import io.circe.{Encoder, Json}
import temple.generate.JsonEncodable

/** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#responseObject */
private[openapi] case class Response(
  description: String,
  content: Map[String, Response.MediaTypeObject],
  required: Option[Boolean] = None,
) extends JsonEncodable.Partial {

  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
    "description" -> Some(description.asJson),
    "required"    -> required.map(_.asJson),
    "content"     -> Some(content.asJson(Encoder.encodeMapLike)),
  )
}

private[openapi] object Response {

  /** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#mediaTypeObject */
  private[openapi] case class MediaTypeObject(schema: OpenAPIType, customFields: (String, Json)*)
      extends JsonEncodable {

    override def jsonEntryIterator: Seq[(String, Json)] = ("schema" -> schema.asJson) +: customFields
  }
}
