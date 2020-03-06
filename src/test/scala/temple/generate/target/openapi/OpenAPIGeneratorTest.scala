package temple.generate.target.openapi

import io.circe.syntax._
import io.circe.yaml.syntax.AsYaml
import org.scalatest.{FlatSpec, Matchers}
import temple.generate.target.openapi.OpenAPIGenerator.generateError

class OpenAPIGeneratorTest extends FlatSpec with Matchers {

  behavior of "OpenAPIGeneratorTest"

  it should "generate error descriptions correctly" in {

    val errorDescription: Response = generateError("this", 500, "This is bad")

    errorDescription.asJson.asYaml.spaces2 shouldBe {
      """description: this
        |content:
        |  application/json:
        |    schema:
        |      type: object
        |      properties:
        |        error:
        |          type: string
        |          example: This is bad
        |""".stripMargin
    }
  }

}
