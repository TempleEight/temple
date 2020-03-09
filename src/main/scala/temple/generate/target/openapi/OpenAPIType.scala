package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._

import scala.collection.immutable.ListMap

sealed abstract private[openapi] class OpenAPIType(fieldEntries: Seq[(String, Json)]) extends JsonEncodable {
  def typeString: String
  final lazy val customFields: Map[String, Json] = fieldEntries.to(ListMap)

  override def toJsonMap: Map[String, Option[Json]] =
    ListMap("type" -> Some(typeString.asJson)) ++ customFields.view.mapValues(Some(_))
}

private[openapi] object OpenAPIType {

  sealed case class OpenAPISimpleType(typeString: String, fieldEntries: (String, Json)*)
      extends OpenAPIType(fieldEntries)

  case class OpenAPIObject(properties: Map[String, OpenAPIType], fieldEntries: (String, Json)*)
      extends OpenAPIType(("properties", properties.asJson) +: fieldEntries) { val typeString = "object" }

  case class OpenAPIArray(items: OpenAPIType, fieldEntries: (String, Json)*)
      extends OpenAPIType(("items", items.asJson) +: fieldEntries) { val typeString = "array" }
}
