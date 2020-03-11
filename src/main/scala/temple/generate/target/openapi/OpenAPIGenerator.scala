package temple.generate.target.openapi

import io.circe.syntax._
import temple.generate.target.openapi.OpenAPIType._

import scala.collection.immutable.ListMap

object OpenAPIGenerator {

  /** Create a Response representation for an error */
  private[openapi] def generateError(description: String, example: String): Response =
    Literal(
      description = description,
      content = ListMap(
        "application/json" -> MediaTypeObject(
          OpenAPIObject(ListMap("error" -> OpenAPISimpleType("string", "example" -> example.asJson))),
        ),
      ),
    )
}
