package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._

import scala.collection.immutable.ListMap

sealed abstract private[openapi] class OpenAPIType(val typeString: String, customFields: Seq[(String, Json)])
    extends JsonEncodable {

  override def toJsonMap: Map[String, Option[Json]] =
    ListMap("type" -> Some(typeString.asJson)) ++ customFields.to(ListMap).view.mapValues(Some(_))
}

private[openapi] object OpenAPIType {

  sealed case class OpenAPISimpleType(override val typeString: String, customFields: (String, Json)*)
      extends OpenAPIType(typeString, customFields)

  case class OpenAPIObject(properties: Map[String, OpenAPIType], customFields: (String, Json)*)
      extends OpenAPIType("object", ("properties", properties.asJson) +: customFields)

  case class OpenAPIArray(items: OpenAPIType, customFields: (String, Json)*)
      extends OpenAPIType("array", ("items", items.asJson) +: customFields)
}
