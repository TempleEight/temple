package temple.generate.target.openapi

import io.circe.Json
import temple.generate.JsonEncodable

// https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#mediaTypeObject
final private[openapi] case class MediaTypeObject(schema: OpenAPIType, customFields: (String, Json)*)
    extends JsonEncodable.Object {
  override def jsonEntryIterator: Seq[(String, Json)] = ("schema" ~> schema) +: customFields
}
