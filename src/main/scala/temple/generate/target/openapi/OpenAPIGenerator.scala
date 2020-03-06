package temple.generate.target.openapi

import temple.generate.target.openapi.OpenAPIType._
import io.circe.syntax._

import scala.collection.immutable.ListMap

object OpenAPIGenerator {

  /** Create a Response representation for an error */
  private[openapi] def generateError(description: String, code: Int, example: String): Response =
    Response(
      description,
      ListMap(
        "application/json" -> Response.MediaTypeObject(
          OpenAPIObject(ListMap("error" -> OpenAPISimpleType("string", "example" -> example.asJson))),
        ),
      ),
    )

}
