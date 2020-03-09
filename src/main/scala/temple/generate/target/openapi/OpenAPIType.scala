package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._

import scala.collection.immutable.ListMap

sealed abstract private[openapi] class OpenAPIType(customFields: Seq[(String, Json)]) extends JsonEncodable {
  def typeString: String

  override def toJsonMap: Map[String, Option[Json]] =
    ListMap("type" -> Some(typeString.asJson)) ++ customFields.to(ListMap).view.mapValues(Some(_))
}

private[openapi] object OpenAPIType {

  sealed case class OpenAPISimpleType(typeString: String, customFields: (String, Json)*)
      extends OpenAPIType(customFields)

  case class OpenAPIObject(properties: Map[String, OpenAPIType], customFields: (String, Json)*)
      extends OpenAPIType(("properties", properties.asJson) +: customFields) { val typeString = "object" }

  case class OpenAPIArray(items: OpenAPIType, customFields: (String, Json)*)
      extends OpenAPIType(("items", items.asJson) +: customFields) { val typeString = "array" }
}
