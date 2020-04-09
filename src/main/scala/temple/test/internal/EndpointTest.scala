package temple.test.internal

import java.text.SimpleDateFormat
import java.util.UUID

import io.circe.{Json, JsonObject}
import temple.ast.{AbstractAttribute, Annotation, AttributeType}
import temple.utils.StringUtils

import scala.util.Try

class TestFailedException(msg: String) extends RuntimeException(msg)

private[internal] class EndpointTest(service: String, endpointName: String) {

  // Validate that the response JSON for the provided key matches the Attribute
  private def validateResponseType(key: String, responseForKey: Json, attribute: AbstractAttribute): Unit =
    attribute.attributeType match {
      case AttributeType.ForeignKey(_) | AttributeType.UUIDType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string, found $responseForKey"))
        assert(Try(UUID.fromString(stringValue)).isSuccess, s"$key is not a valid UUID, found $responseForKey")
      case AttributeType.BoolType =>
        assert(responseForKey.isBoolean, s"$key is not of type bool, found $responseForKey")
      case AttributeType.DateType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string, found $responseForKey"))
        assert(
          Try(new SimpleDateFormat("yyyy-MM-dd").parse(stringValue)).isSuccess,
          s"$key is not a valid date, found $responseForKey",
        )
      case AttributeType.DateTimeType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string, found $responseForKey"))
        assert(
          Try(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(stringValue)).isSuccess,
          s"$key is not a valid datetime, found $responseForKey",
        )
      case AttributeType.TimeType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string, found $responseForKey"))
        assert(
          Try(new SimpleDateFormat("HH:mm:ss").parse(stringValue)).isSuccess,
          s"$key is not a valid time, found $responseForKey",
        )
      case AttributeType.BlobType(_) =>
        assert(responseForKey.isString, s"$key is not a valid string, found $responseForKey")
      case AttributeType.StringType(max, min) =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string, found $responseForKey"))
        min.foreach { minValue =>
          assert(stringValue.length >= minValue, s"$key does not satisfy min bound of $minValue, found $stringValue")
        }
        max.foreach { maxValue =>
          assert(stringValue.length <= maxValue, s"$key does not satisfy max bound of $maxValue, found $stringValue")
        }
      case AttributeType.IntType(max, min, _) =>
        // TODO: precision
        val intValue =
          responseForKey.asNumber.flatMap(_.toInt).getOrElse(fail(s"$key is not a valid number, found $responseForKey"))
        min.foreach { minValue =>
          assert(intValue >= minValue, s"$key does not satisfy min bound of $minValue, found $intValue")
        }
        max.foreach { maxValue =>
          assert(intValue <= maxValue, s"$key does not satisfy max bound of $maxValue, found $intValue")
        }
      case AttributeType.FloatType(max, min, _) =>
        // TODO: precision
        val floatValue =
          responseForKey.asNumber.map(_.toFloat).getOrElse(fail(s"$key is not a valid float, found $responseForKey"))
        min.foreach { minValue =>
          assert(floatValue >= minValue, s"$key does not satisfy min bound of $minValue, found $floatValue")
        }
        max.foreach { maxValue =>
          assert(floatValue <= maxValue, s"$key does not satisfy max bound of $maxValue, found $floatValue")
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
        validateResponseType(name, responseForKey, attribute)

        request.foreach { request =>
          // Where the attribute is not @serverSet, validate that what was sent is exactly what is returned
          if (!attribute.accessAnnotation.contains(Annotation.ServerSet)) {
            val requestForKey = request(name).getOrElse(fail(s"request was expected to contain key $name, but didn't"))
            assertEqual(requestForKey, responseForKey)
          }
        }
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
