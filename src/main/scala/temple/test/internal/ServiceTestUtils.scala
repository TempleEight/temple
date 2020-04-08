package temple.test.internal

import java.sql.{Date, Time, Timestamp}
import java.util.UUID

import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Json, JsonObject}
import scalaj.http.Http
import temple.ast.AbstractServiceBlock.ServiceBlock
import temple.ast.{AbstractAttribute, Annotation, AttributeType, Metadata}
import temple.utils.MonadUtils.FromEither
import temple.utils.StringUtils

import scala.util.Random

object ServiceTestUtils {
  case class CreateResponse(id: String, accessToken: String)

  // Decode a string body into a JSON object, failing the test if any stage fails
  private def decodeJSON(test: EndpointTest, body: String): JsonObject = {
    val parsedBody = parse(body) fromEither { failure =>
      test.fail(s"response was not valid JSON - ${failure.message}")
    }

    parsedBody.asObject getOrElse {
      test.fail("response was not a JSON object")
    }
  }

  private def executeRequest(method: String, test: EndpointTest, url: String, token: String): JsonObject = {
    val response = Http(url).method(method).header("Authorization", s"Bearer $token").asString
    test.assertEqual(200, response.code)
    decodeJSON(test, response.body)
  }

  private def executeRequest(method: String, test: EndpointTest, url: String, body: Json, token: String) = {
    val response = Http(url).postData(body.toString).method(method).header("Authorization", s"Bearer $token").asString
    test.assertEqual(200, response.code)
    decodeJSON(test, response.body)
  }

  /**
    * Execute a POST request, where the response body is expected to be a JSON object
    * @param url The URL to send the request to, prefixed with the protocol
    * @param body The JSON body to send along with the request
    * @param test The endpoint under test
    * @return The decoded JSON response from the request
    */
  def postRequest(test: EndpointTest, url: String, body: Json, token: String = ""): JsonObject =
    executeRequest("POST", test, url, body, token)

  /**
    * Execute a PUT request, where the response body is expected to be a JSON object
    * @param url The URL to send the request to, prefixed with the protocol
    * @param body The JSON body to send along with the request
    * @param test The endpoint under test
    * @return The decoded JSON response from the request
    */
  def putRequest(test: EndpointTest, url: String, body: Json, token: String = ""): JsonObject =
    executeRequest("PUT", test, url, body, token)

  /**
    * Execute a GET request, where the response body is expected to be a JSON object
    * @param url The URL to send the request to, prefixed with the protocol
    * @param test The endpoint under test
    * @return The decoded JSON response from the request
    */
  def getRequest(test: EndpointTest, url: String, token: String = ""): JsonObject =
    executeRequest("GET", test, url, token)

  /**
    * Execute a DELETE request, where the response body is expected to be a JSON object
    * @param url The URL to send the request to, prefixed with the protocol
    * @param test The endpoint under test
    * @return The decoded JSON response from the request
    */
  def deleteRequest(test: EndpointTest, url: String, token: String = ""): JsonObject =
    executeRequest("DELETE", test, url, token)

  /**
    * Create an access token for use by the provided service
    * @param service The service the access token is required for
    * @param baseURL The base URL to execute requests
    * @return The access token, as a string
    */
  def getAuthTokenWithEmail(service: String, baseURL: String): String = {
    val test = new EndpointTest(service, "fetch auth token")
    val registerJson = ServiceTestUtils
      .postRequest(
        test,
        s"http://$baseURL/api/auth/register",
        Map("email" -> randomEmail(), "password" -> StringUtils.randomString(10)).asJson,
      )
    registerJson("AccessToken").flatMap(_.asString).getOrElse(test.fail("access token was not a valid string"))
  }

  /**
    * Construct a valid request body for the provided attributes
    * @param test The endpoint under test
    * @param attributes The attributes to be included in the body
    * @param allServices All other services in the system - needed if any foreign keys are present
    * @param baseURL The base URL where requests should be executed
    * @param accessToken The access token used for requests
    * @return
    */
  def constructRequestBody(
    test: EndpointTest,
    attributes: Map[String, AbstractAttribute],
    allServices: Map[String, ServiceBlock],
    baseURL: String,
    accessToken: String,
  ): Json =
    // Drop any attributes that are @server or @serverSet
    attributes
      .filterNot {
        case (_, attribute) =>
          attribute.accessAnnotation.contains(Annotation.Server) || attribute.accessAnnotation.contains(
            Annotation.ServerSet,
          )
      }
      .map {
        case (name, attribute) =>
          val value: Json = attribute.attributeType match {
            case AttributeType.ForeignKey(references) =>
              create(test, references, allServices, baseURL, accessToken).id.asJson
            case AttributeType.UUIDType =>
              UUID.randomUUID().asJson
            case AttributeType.BoolType =>
              Random.nextBoolean().asJson
            case AttributeType.DateType =>
              new Date(Random.nextLong()).toString.asJson
            case AttributeType.DateTimeType =>
              new Timestamp(Random.nextLong()).toString.asJson
            case AttributeType.TimeType =>
              new Time(Random.nextLong()).toString.asJson
            case AttributeType.BlobType(_) =>
              // TODO
              "todo".asJson
            case AttributeType.StringType(max, min) =>
              val maxValue: Long = max.getOrElse(20)
              val minValue: Int  = min.getOrElse(0)
              val random         = Random.between(minValue, maxValue)
              StringUtils.randomString(random.toInt).asJson
            case AttributeType.IntType(max, min, _) =>
              // TODO: Switch on precision
              val maxValue: Long = max.getOrElse(Long.MaxValue)
              val minValue: Long = min.getOrElse(Long.MinValue)
              Random.between(minValue, maxValue).asJson
            case AttributeType.FloatType(max, min, _) =>
              // TODO: Switch on precision
              val maxValue = max.getOrElse(Double.MaxValue)
              val minValue = min.getOrElse(Double.MinValue)
              Random.between(minValue, maxValue).asJson
          }
          (name, value)
      }
      .asJson

  /** Create a new object in a given service, returning the ID field and access token used to make the request */
  def create(
    test: EndpointTest,
    serviceName: String,
    allServices: Map[String, ServiceBlock],
    baseURL: String,
    accessToken: String,
  ): CreateResponse = {
    val service = allServices.getOrElse(serviceName, test.fail(s"service $serviceName does not exist"))
    // If this service is an auth service, the same access token cannot be used twice, so make a new one to be safe...
    // This is so that services that reference 2+ auth'd services can be successfully tested
    val newAccessToken = service.lookupMetadata[Metadata.ServiceAuth].fold(accessToken) { _ =>
      ServiceTestUtils.getAuthTokenWithEmail(serviceName, baseURL)
    }

    val requestBody = constructRequestBody(test, service.attributes, allServices, baseURL, newAccessToken)
    val createJSON = ServiceTestUtils
      .postRequest(
        test,
        s"http://$baseURL/api/${serviceName.toLowerCase}",
        requestBody,
        newAccessToken,
      )
    val idJSON = createJSON("ID").getOrElse(test.fail(s"response to create $serviceName did not contain an ID key"))
    val id =
      idJSON.asString.getOrElse(test.fail(s"response to create $serviceName contained field ID, but was not a string"))
    CreateResponse(id, newAccessToken)
  }

  /**
    * Generate a random email address
    */
  def randomEmail(): String = {
    val name   = StringUtils.randomString(10)
    val domain = StringUtils.randomString(8)
    s"$name@$domain.com"
  }
}
