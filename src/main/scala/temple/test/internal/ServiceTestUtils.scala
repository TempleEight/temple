package temple.test.internal

import java.net.HttpURLConnection
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.UUID

import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Json, JsonObject}
import scalaj.http.Http
import temple.ast.AbstractServiceBlock.ServiceBlock
import temple.ast.{AbstractAttribute, AttributeType, Metadata}
import temple.utils.MonadUtils.FromEither
import temple.utils.StringUtils

import scala.util.Random

object ServiceTestUtils {

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
    val response     = Http(url).method(method).header("Authorization", s"Bearer $token").asString
    val jsonResponse = decodeJSON(test, response.body)
    test.assertEqual(200, response.code, s"Response from $url was: $jsonResponse")
    jsonResponse
  }

  private def executeRequest(method: String, test: EndpointTest, url: String, body: Json, token: String) = {
    val response     = Http(url).postData(body.toString).method(method).header("Authorization", s"Bearer $token").asString
    val jsonResponse = decodeJSON(test, response.body)
    test.assertEqual(200, response.code, s"Response from $url was: $jsonResponse,\nwith request body: $body")
    jsonResponse
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
      .filter { case (_, attribute) => attribute.inRequest }
      .map {
        case (name, attribute) =>
          val value: Json = attribute.attributeType match {
            case AttributeType.ForeignKey(references) =>
              create(test, references, allServices, baseURL, accessToken).asJson
            case AttributeType.UUIDType =>
              UUID.randomUUID().asJson
            case AttributeType.BoolType =>
              Random.nextBoolean().asJson
            case AttributeType.DateType =>
              new SimpleDateFormat("yyyy-MM-dd").format(new Date(Random.between(0, 100000000000000L))).asJson
            case AttributeType.DateTimeType =>
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                .format(new Date(Random.between(0, 100000000000000L)))
                .asJson
            case AttributeType.TimeType =>
              new SimpleDateFormat("HH:mm:ss").format(new Date(Random.between(0, 100000000000000L))).asJson
            case AttributeType.BlobType(_) =>
              // TODO
              "todo".asJson
            case AttributeType.StringType(max, min) =>
              val maxValue: Long = max.getOrElse(20)
              val minValue: Int  = min.getOrElse(0)
              val random         = Random.between(minValue, maxValue + 1)
              StringUtils.randomString(random.toInt).asJson
            case intType: AttributeType.IntType =>
              Random.between(intType.minValue, intType.maxValue + 1).asJson
            case floatType: AttributeType.FloatType =>
              Random.between(floatType.minValue, floatType.maxValue).asJson
          }
          (name, value)
      }
      .asJson

  /** Create an object in a given service, returning the unique ID for that object */
  private def createObject(
    test: EndpointTest,
    service: ServiceBlock,
    serviceName: String,
    allServices: Map[String, ServiceBlock],
    baseURL: String,
    accessToken: String,
  ): JsonObject = {
    val requestBody = constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
    postRequest(test, s"http://$baseURL/api/${StringUtils.kebabCase(serviceName)}", requestBody, accessToken)
  }

  /** Create a new object in a given service, returning the ID field and access token used to make the request */
  def create(
    test: EndpointTest,
    serviceName: String,
    allServices: Map[String, ServiceBlock],
    baseURL: String,
    accessToken: String,
  ): String = {
    val service = allServices.getOrElse(serviceName, test.fail(s"service $serviceName does not exist"))

    // If this service is an auth service, an access token can only create one entity
    val createJSON = service.lookupLocalMetadata[Metadata.ServiceAuth] match {
      case Some(_) =>
        // First check if that single entity already exists
        val response = Http(s"http://$baseURL/api/${StringUtils.kebabCase(serviceName)}")
          .method("GET")
          .header("Authorization", s"Bearer $accessToken")
          .asString
        response.code match {
          case HttpURLConnection.HTTP_MOVED_TEMP =>
            // Already exists, so perform a GET on it now the ID is known
            val url = response.header("Location").getOrElse(test.fail("302 response without Location header"))
            getRequest(test, s"http://$url", accessToken)
          case HttpURLConnection.HTTP_NOT_FOUND =>
            // Not found, so create a new one
            createObject(test, service, serviceName, allServices, baseURL, accessToken)
          case _ =>
            test.fail(s"Unexpected response code ${response.code} when creating $serviceName")
        }
      case None =>
        createObject(test, service, serviceName, allServices, baseURL, accessToken)
    }

    val idJSON = createJSON("id").getOrElse(test.fail(s"response to create $serviceName did not contain an id key"))
    idJSON.asString.getOrElse(test.fail(s"response to create $serviceName contained field id, but was not a string"))
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
