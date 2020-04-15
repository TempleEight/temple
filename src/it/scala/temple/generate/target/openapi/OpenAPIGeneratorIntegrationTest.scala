package temple.generate.target.openapi

import org.scalatest.Matchers
import temple.ast.AbstractAttribute.Attribute
import temple.ast.Annotation
import temple.ast.AttributeType.{BlobType, BoolType, DateTimeType, DateType, FloatType, ForeignKey, IntType, StringType, TimeType}
import temple.containers.SwaggerSpec
import temple.generate.CRUD
import temple.generate.target.openapi.ast.{OpenAPIRoot, Service}

import scala.collection.immutable.ListMap

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

  it should "succeed when generating a service schema with all endpoints" in {
    val openAPIFiles = OpenAPIGenerator.generate(
      OpenAPIRoot.build("x", "0.1.2")(
        Service(
          "match",
          CRUD.values,
          ListMap(
            "a" -> Attribute(IntType()),
            "b" -> Attribute(FloatType()),
            "c" -> Attribute(BoolType),
            "d" -> Attribute(DateType),
            "e" -> Attribute(TimeType),
            "f" -> Attribute(DateTimeType, accessAnnotation = Some(Annotation.Server)),
            "g" -> Attribute(DateTimeType),
            "h" -> Attribute(BlobType(), accessAnnotation = Some(Annotation.ServerSet)),
            "i" -> Attribute(StringType(), accessAnnotation = Some(Annotation.Client)),
            "j" -> Attribute(ForeignKey("User")),
          ),
        ),
      ),
    )
    validate(openAPIFiles.values.head) shouldBe valid
  }

}
