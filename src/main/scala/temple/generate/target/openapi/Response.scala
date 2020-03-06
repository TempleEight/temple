package temple.generate.target.openapi

import io.circe.syntax._

import io.circe.{Encoder, Json}

import scala.collection.immutable.ListMap

/** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#responseObject */
private[openapi] case class Response(
  description: String,
  content: Map[String, Response.MediaTypeObject],
  required: Option[Boolean] = None,
) extends Jsonable {

  override def toJsonMap: Map[String, Option[Json]] = ListMap(
    "description" -> Some(description.asJson),
    "required"    -> required.map(_.asJson),
    "content"     -> Some(content.asJson(Encoder.encodeMapLike)),
  )
}

private[openapi] object Response {

  /** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#mediaTypeObject */
  private[openapi] case class MediaTypeObject(schema: OpenAPIType, fieldEntries: (String, Json)*) extends Jsonable {
    final lazy val customFields: Map[String, Json] = fieldEntries.to(ListMap)

    override def toJsonMap: Map[String, Option[Json]] =
      ListMap(
        "schema" -> Some(schema.asJson),
      ) ++ customFields.view.mapValues(value => Some(value.asJson))
  }
}
