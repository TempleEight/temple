package temple.generate.target.openapi

import io.circe.syntax._
import io.circe.yaml.syntax.AsYaml
import org.scalatest.{FlatSpec, Matchers}
import temple.ast
import temple.ast.AttributeType._
import temple.ast.{Annotation, Attribute}
import temple.generate.CRUD
import temple.generate.target.openapi.OpenAPIGenerator.generateError
import temple.generate.target.openapi.ast.{Response, Service}

import scala.collection.immutable.ListMap

class OpenAPIGeneratorTest extends FlatSpec with Matchers {

  behavior of "OpenAPIGenerator"

  it should "generate error descriptions correctly" in {

    val errorDescription: Response = generateError("this", "This is bad")

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

  it should "generate OpenAPI specs correctly" in {
    val openAPI = OpenAPIGenerator.render("x", "0.1.2")(
      Service(
        "match",
        CRUD.values.toSet,
        ListMap(
          "a" -> Attribute(IntType()),
          "b" -> ast.Attribute(FloatType()),
          "c" -> ast.Attribute(BoolType),
          "d" -> ast.Attribute(DateType),
          "e" -> ast.Attribute(TimeType),
          "f" -> ast.Attribute(DateTimeType, accessAnnotation = Some(Annotation.Server)),
          "g" -> ast.Attribute(DateTimeType),
          "h" -> ast.Attribute(BlobType(), accessAnnotation = Some(Annotation.ServerSet)),
          "i" -> ast.Attribute(StringType(), accessAnnotation = Some(Annotation.Client)),
          "j" -> ast.Attribute(ForeignKey("User")),
        ),
      ),
    )
    openAPI shouldBe {
      """openapi: 3.0.0
        |info:
        |  title: x
        |  version: 0.1.2
        |paths:
        |  /match:
        |    post:
        |      summary: Register a new match
        |      tags:
        |      - Match
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              properties:
        |                a:
        |                  type: number
        |                  format: int32
        |                b:
        |                  type: number
        |                  format: double
        |                c:
        |                  type: boolean
        |                d:
        |                  type: string
        |                  format: date
        |                e:
        |                  type: string
        |                  format: time
        |                g:
        |                  type: string
        |                  format: date-time
        |                i:
        |                  type: string
        |                j:
        |                  type: number
        |                  format: int32
        |                  description: Reference to User ID
        |      responses:
        |        '200':
        |          description: Match successfully created
        |          content:
        |            application/json:
        |              schema:
        |                type: object
        |                properties:
        |                  a:
        |                    type: number
        |                    format: int32
        |                  b:
        |                    type: number
        |                    format: double
        |                  c:
        |                    type: boolean
        |                  d:
        |                    type: string
        |                    format: date
        |                  e:
        |                    type: string
        |                    format: time
        |                  g:
        |                    type: string
        |                    format: date-time
        |                  h:
        |                    type: string
        |                  i:
        |                    type: string
        |                  j:
        |                    type: number
        |                    format: int32
        |                    description: Reference to User ID
        |        '400':
        |          $ref: '#/components/responses/Error400'
        |        '500':
        |          $ref: '#/components/responses/Error500'
        |  /match/{id}:
        |    parameters:
        |    - in: path
        |      name: id
        |      schema:
        |        type: number
        |        format: int32
        |      required: true
        |      description: ID of the match to perform operations on
        |    get:
        |      summary: Look up a single match
        |      tags:
        |      - Match
        |      responses:
        |        '200':
        |          description: Match details
        |          content:
        |            application/json:
        |              schema:
        |                type: object
        |                properties:
        |                  a:
        |                    type: number
        |                    format: int32
        |                  b:
        |                    type: number
        |                    format: double
        |                  c:
        |                    type: boolean
        |                  d:
        |                    type: string
        |                    format: date
        |                  e:
        |                    type: string
        |                    format: time
        |                  g:
        |                    type: string
        |                    format: date-time
        |                  h:
        |                    type: string
        |                  i:
        |                    type: string
        |                  j:
        |                    type: number
        |                    format: int32
        |                    description: Reference to User ID
        |        '400':
        |          $ref: '#/components/responses/Error400'
        |        '500':
        |          $ref: '#/components/responses/Error500'
        |  /match/all:
        |    get:
        |      summary: Get a list of every match
        |      tags:
        |      - Match
        |      responses:
        |        '200':
        |          description: Match list successfully fetched
        |          content:
        |            application/json:
        |              schema:
        |                type: array
        |                items:
        |                  type: object
        |                  properties:
        |                    a:
        |                      type: number
        |                      format: int32
        |                    b:
        |                      type: number
        |                      format: double
        |                    c:
        |                      type: boolean
        |                    d:
        |                      type: string
        |                      format: date
        |                    e:
        |                      type: string
        |                      format: time
        |                    g:
        |                      type: string
        |                      format: date-time
        |                    h:
        |                      type: string
        |                    i:
        |                      type: string
        |                    j:
        |                      type: number
        |                      format: int32
        |                      description: Reference to User ID
        |        '500':
        |          $ref: '#/components/responses/Error500'
        |components:
        |  responses:
        |    Error400:
        |      description: Invalid request
        |      content:
        |        application/json:
        |          schema:
        |            type: object
        |            properties:
        |              error:
        |                type: string
        |                example: 'Invalid request parameters: name'
        |    Error500:
        |      description: The server encountered an error while serving this request
        |      content:
        |        application/json:
        |          schema:
        |            type: object
        |            properties:
        |              error:
        |                type: string
        |                example: 'Unable to reach user service: connection timeout'
        |""".stripMargin
    }
  }

}
