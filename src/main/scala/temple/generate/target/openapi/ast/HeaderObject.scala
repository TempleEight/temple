package temple.generate.target.openapi.ast

import io.circe.Json
import temple.generate.JsonEncodable

/** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#header-object */
case class HeaderObject(
  description: String,
  typ: String,
) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "description" ~> description,
    "schema" ~> Map(
      "type" ~> typ,
    ),
  )
}
