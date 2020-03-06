package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._
import temple.generate.target.openapi.OpenAPIType.{OpenAPIArray, OpenAPIObject}
import temple.utils.MonadUtils.MatchPartial

import scala.collection.immutable.ListMap

sealed abstract private[openapi] class OpenAPIType(fieldEntries: Seq[(String, Json)]) extends Jsonable {
  def typeString: String
  final lazy val customFields: Map[String, Json] = fieldEntries.to(ListMap)

  override def toJsonMap: Map[String, Option[Json]] =
    ListMap(
      "type"       -> Some(typeString.asJson),
      "properties" -> this.matchPartial { case openAPIType: OpenAPIObject => openAPIType.properties.asJson },
      "items"      -> this.matchPartial { case openAPIType: OpenAPIArray => openAPIType.items.asJson },
    ) ++ customFields.view.mapValues(value => Some(value.asJson))
}

private[openapi] object OpenAPIType {

  sealed case class OpenAPISimpleType(typeString: String, fieldEntries: (String, Json)*)
      extends OpenAPIType(fieldEntries)

  case class OpenAPIObject(properties: Map[String, OpenAPIType], fieldEntries: (String, Json)*)
      extends OpenAPIType(fieldEntries) { val typeString = "object" }

  case class OpenAPIArray(items: OpenAPIType, fieldEntries: (String, Json)*) extends OpenAPIType(fieldEntries) {
    val typeString = "array"
  }
}
