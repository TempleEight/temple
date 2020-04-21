package temple.generate.target.openapi.ast

import io.circe.Json
import temple.generate.JsonEncodable

/** https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#security-scheme-object */
case class SecurityScheme(typ: String, scheme: String, bearerFormat: String) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "type"         ~> typ,
    "scheme"       ~> scheme,
    "bearerFormat" ~> bearerFormat,
  )
}
