package temple.test.internal

import java.text.SimpleDateFormat
import java.util.UUID

import io.circe.{Json, JsonObject}
import temple.ast.{AbstractAttribute, Annotation, AttributeType}
import temple.utils.StringUtils

class TestFailedException(msg: String) extends RuntimeException(msg)

private[internal] class EndpointTest(service: String, endpointName: String) {

  // Validate that the response JSON for the provided key matches the Attribute
  private def validateResponseType(
    key: String,
    requestForKey: Option[Json],
    responseForKey: Json,
    attribute: AbstractAttribute,
  ): Unit =
    attribute.attributeType match {
      case AttributeType.ForeignKey(_) | AttributeType.UUIDType =>
        val responseUUID = responseForKey.asString
          .map(UUID.fromString)
          .getOrElse(fail(s"$key in response is not a valid uuid, found $responseForKey"))
        requestForKey
          .foreach { json =>
            val requestUUID = json.asString
              .map(UUID.fromString)
              .getOrElse(fail(s"$key in request is not a valid uuid, found $requestForKey"))
            assertEqual(requestUUID, responseUUID)
          }
      case AttributeType.BoolType =>
        val responseBool =
          responseForKey.asBoolean.getOrElse(fail(s"$key in response is not of type bool, found $responseForKey"))
        requestForKey.foreach { json =>
          val requestBool =
            json.asBoolean.getOrElse(fail(s"$key in response is not of type bool, found $requestForKey"))
          assertEqual(requestBool, responseBool)
        }
      case AttributeType.DateType =>
        val responseDate = responseForKey.asString
          .map(new SimpleDateFormat("yyyy-MM-dd").parse)
          .getOrElse(fail(s"$key is not a valid date, found $responseForKey"))
        requestForKey.foreach { json =>
          val requestDate =
            json.asString
              .map(new SimpleDateFormat("yyyy-MM-dd").parse)
              .getOrElse(fail(s"$key in response is not of type date, found $requestForKey"))
          assertEqual(requestDate, responseDate)
        }
      case AttributeType.DateTimeType =>
        val responseDateTime = responseForKey.asString
          .map(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse)
          .getOrElse(fail(s"$key is not a valid datetime, found $responseForKey"))
        requestForKey.foreach { json =>
          val requestDateTime =
            json.asString
              .map(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse)
              .getOrElse(fail(s"$key in response is not a valid datetime, found $requestForKey"))
          assertEqual(requestDateTime, responseDateTime)
        }
      case AttributeType.TimeType =>
        val responseTime = responseForKey.asString
          .map(new SimpleDateFormat("HH:mm:ss").parse)
          .getOrElse(fail(s"$key is not a valid time, found $responseForKey"))
        requestForKey.foreach { json =>
          val requestTime =
            json.asString
              .map(new SimpleDateFormat("HH:mm:ss").parse)
              .getOrElse(fail(s"$key in response is not a valid time, found $requestForKey"))
          assertEqual(requestTime, responseTime)
        }
      case AttributeType.BlobType(_) =>
        val responseBlob = responseForKey.asString.getOrElse(fail(s"$key is not a valid string, found $responseForKey"))
        requestForKey.foreach { json =>
          val requestBlob =
            json.asString
              .getOrElse(fail(s"$key in response is not a valid string, found $requestForKey"))
          assertEqual(requestBlob, responseBlob)
        }
      case AttributeType.StringType(max, min) =>
        val responseString =
          responseForKey.asString.getOrElse(fail(s"$key is not a valid string, found $responseForKey"))
        min.foreach { minValue =>
          assert(
            responseString.length >= minValue,
            s"$key does not satisfy min bound of $minValue, found $responseString",
          )
        }
        max.foreach { maxValue =>
          assert(
            responseString.length <= maxValue,
            s"$key does not satisfy max bound of $maxValue, found $responseString",
          )
        }
        requestForKey.foreach { json =>
          val requestString =
            json.asString.getOrElse(fail(s"$key in response is not a valid string, found $requestForKey"))
          assertEqual(requestString, responseString)
        }
      case AttributeType.IntType(max, min, _) =>
        // TODO: precision
        val responseInt =
          responseForKey.asNumber.flatMap(_.toInt).getOrElse(fail(s"$key is not a valid int, found $responseForKey"))
        min.foreach { minValue =>
          assert(responseInt >= minValue, s"$key does not satisfy min bound of $minValue, found $responseInt")
        }
        max.foreach { maxValue =>
          assert(responseInt <= maxValue, s"$key does not satisfy max bound of $maxValue, found $responseInt")
        }
        requestForKey.foreach { json =>
          val requestInt =
            json.asNumber
              .flatMap(_.toInt)
              .getOrElse(fail(s"$key in response is not a valid int, found $requestForKey"))
          assertEqual(requestInt, responseInt)
        }
      case AttributeType.FloatType(max, min, _) =>
        // TODO: precision
        val responseFloat =
          responseForKey.asNumber.map(_.toFloat).getOrElse(fail(s"$key is not a valid float, found $responseForKey"))
        min.foreach { minValue =>
          assert(responseFloat >= minValue, s"$key does not satisfy min bound of $minValue, found $responseFloat")
        }
        max.foreach { maxValue =>
          assert(responseFloat <= maxValue, s"$key does not satisfy max bound of $maxValue, found $responseFloat")
        }
        requestForKey.foreach { json =>
          val requestFloat =
            json.asNumber
              .map(_.toFloat)
              .getOrElse(fail(s"$key in response is not a valid float, found $requestForKey"))
          assertEqual(requestFloat, responseFloat)
        }
    }

  def validateResponseBody(
    request: Option[JsonObject],
    response: JsonObject,
    attributes: Map[String, AbstractAttribute],
  ): Unit = {
    // The response should contain exactly the keys in attributes, PLUS an ID attribute, MINUS anything that is @server
    val expectedAttributes     = attributes.filter { case (_, attribute) => attribute.inResponse }
    val expectedAttributeNames = expectedAttributes.keys.toSet
    val foundAttributeNames    = response.keys.toSet
    assertEqual(expectedAttributeNames, foundAttributeNames)

    expectedAttributes.foreach {
      case (name, attribute) =>
        // Validate that the response JSON is of the expected types
        val responseForKey = response(name).getOrElse(fail(s"could not find key $name in response body"))
        val requestForKey  = request.flatMap(json => json(name))
        validateResponseType(name, requestForKey, responseForKey, attribute)
    }
  }

  def assertEqual(expected: Any, found: Any, message: String = ""): Unit =
    if (!expected.equals(found))
      fail(s"expected $expected but found $found.\n$message")

  def assert(value: Boolean, failMsg: String): Unit =
    if (!value) fail(failMsg)

  def pass(): Unit =
    println(StringUtils.indent(s"✅ $service $endpointName", 4))

  def fail(message: String): Nothing =
    throw new TestFailedException(StringUtils.indent(s"❌ $service $endpointName: $message", 4))
}
