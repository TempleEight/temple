package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable

sealed abstract private[openapi] class OpenAPIType(val typeString: String, customFields: Seq[(String, Json)])
    extends JsonEncodable.Object {

  override def jsonEntryIterator: Seq[(String, Json)] = ("type" -> typeString.asJson) +: customFields
}

private[openapi] object OpenAPIType {

  sealed case class OpenAPISimpleType(override val typeString: String, customFields: (String, Json)*)
      extends OpenAPIType(typeString, customFields)

  object OpenAPISimpleType {

    def apply(typeString: String, format: String, customFields: (String, Json)*): OpenAPISimpleType =
      new OpenAPISimpleType(typeString, ("format" -> format.asJson) +: customFields: _*)
  }

  case class OpenAPIObject(properties: Map[String, OpenAPIType], customFields: (String, Json)*)
      extends OpenAPIType("object", ("properties", properties.asJson) +: customFields)

  case class OpenAPIArray(items: OpenAPIType, customFields: (String, Json)*)
      extends OpenAPIType("array", ("items", items.asJson) +: customFields)
}
