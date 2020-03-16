package temple.generate.target.openapi

import org.scalatest.Matchers
import temple.containers.SwaggerSpec

class OpenAPIGeneratorIntegrationTest extends SwaggerSpec with Matchers {
  behavior of "OpenAPIValidator"

  it should "succeed when a correct schema is used" in {
    val schema: String =
      """openapi: "3.0.0"
        |info:
        |  title: Hello World
        |  version: "1.0"
        |paths:
        |  /greeting:
        |    get:
        |      summary: Generate greeting
        |      description: Generates a greeting message
        |      responses:
        |        200:
        |          description: greeting response
        |          content:
        |            text/plain:
        |              schema:
        |                type: string
        |                example: "hello!"
        |""".stripMargin
    validate(schema) shouldBe valid
  }

  it should "fail when an invalid schema is used" in {
    val schema: String = "randomStringContents"
    validate(schema) should not be valid
  }

}
