package temple.test.internal

import io.circe.parser.parse
import io.circe.{Json, JsonObject}
import scalaj.http.Http
import temple.utils.StringUtils

object ServiceTestUtils {

  /**
    * Execute a POST request, where the response body is expected to be a JSON object
    * @param url The URL to send the request to, prefixed with the protocol
    * @param body The JSON body to send along with the request
    * @param test The endpoint under test
    * @return The decoded JSON response from the request
    */
  def postRequest(url: String, body: Json, test: EndpointTest): JsonObject = {
    val response = Http(url).postData(body.toString).method("POST").asString
    test.assertEqual(200, response.code)

    parse(response.body) match {
      case Left(value) =>
        test.fail(s"response was not valid JSON: ${value.message}")
      case Right(value) =>
        value.asObject match {
          case Some(value) => value
          case None        => test.fail("response was not a JSON object")
        }
    }
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
