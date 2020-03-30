package temple.test.internal

import java.text.SimpleDateFormat
import java.util.UUID

import io.circe.{Json, JsonObject}
import temple.ast.{Annotation, Attribute, AttributeType}
import temple.utils.StringUtils

import scala.util.Try

private[internal] class EndpointTest(service: String, endpointName: String) {
  class TestFailedException(msg: String) extends RuntimeException(msg)

  // Validate that the response JSON for the provided key matches the Attribute
  private def validateResponseType(key: String, responseForKey: Json, attribute: Attribute): Unit =
    attribute.attributeType match {
      case AttributeType.ForeignKey(_) | AttributeType.UUIDType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string"))
        assert(Try(UUID.fromString(stringValue)).isSuccess, s"$key is not a valid UUID")
      case AttributeType.BoolType =>
        assert(responseForKey.isBoolean, s"$key is not of type bool")
      case AttributeType.DateType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string"))
        assert(Try(new SimpleDateFormat("yyyy-MM-dd").parse(stringValue)).isSuccess, s"$key is not a valid date")
      case AttributeType.DateTimeType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string"))
        assert(
          Try(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(stringValue)).isSuccess,
          s"$key is not a valid datetime",
        )
      case AttributeType.TimeType =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string"))
        assert(Try(new SimpleDateFormat("HH:mm:ssXXX").parse(stringValue)).isSuccess, s"$key is not a valid time")
      case AttributeType.BlobType(_) =>
        assert(responseForKey.isString, s"$key is not a valid string")
      case AttributeType.StringType(max, min) =>
        val stringValue = responseForKey.asString.getOrElse(fail(s"$key is not a valid string"))
        min.foreach { minValue =>
          assert(stringValue.length >= minValue, s"$key does not satisfy min bound of $minValue")
        }
        max.foreach { maxValue =>
          assert(stringValue.length <= maxValue, s"$key does not satisfy max bound of $maxValue")
        }
      case AttributeType.IntType(max, min, _) =>
        // TODO: precision
        val intValue = responseForKey.asNumber.flatMap(_.toInt).getOrElse(fail(s"$key is not a valid number"))
        min.foreach { minValue =>
          assert(intValue >= minValue, s"$key does not satisfy min bound of $minValue")
        }
        max.foreach { maxValue =>
          assert(intValue <= maxValue, s"$key does not satisfy max bound of $maxValue")
        }
      case AttributeType.FloatType(max, min, _) =>
        // TODO: precision
        val floatValue = responseForKey.asNumber.map(_.toFloat).getOrElse(fail(s"$key is not a valid float"))
        min.foreach { minValue =>
          assert(floatValue >= minValue, s"$key does not satisfy min bound of $minValue")
        }
        max.foreach { maxValue =>
          assert(floatValue <= maxValue, s"$key does not satisfy max bound of $maxValue")
        }
    }

  def validateResponseBody(request: JsonObject, response: JsonObject, attributes: Map[String, Attribute]): Unit = {
    // The response should contain exactly the keys in attributes, PLUS an ID attribute, MINUS anything that is @server
    val expectedAttributes = attributes.filter {
      case (_, attribute) =>
        attribute.accessAnnotation.fold(true) {
          case Annotation.Server                        => false
          case Annotation.ServerSet | Annotation.Client => true
        }
    }
    val expectedAttributeNames = (expectedAttributes.keys ++ Seq("ID")).map(_.capitalize).toSet
    val foundAttributeNames    = response.keys.toSet
    assertEqual(expectedAttributeNames, foundAttributeNames)

    expectedAttributes.foreach {
      case (name, attribute) =>
        // Validate that the response JSON is of the expected types
        val responseForKey = response(name.capitalize).getOrElse(fail(s"could not find key $name in response body"))
        validateResponseType(name, responseForKey, attribute)

        // Where the attribute is not @serverSet, validate that what was sent is exactly what is returned
        val shouldValidateRequest = attribute.accessAnnotation.fold(true) {
          case Annotation.Server | Annotation.ServerSet => false
          case Annotation.Client                        => true
        }
        if (shouldValidateRequest) {
          val requestForKey = request(name).getOrElse(fail(s"request was expected to contain key $name, but didn't"))
          assertEqual(requestForKey, responseForKey)
        }
    }
  }

  def assertEqual(expected: Any, found: Any): Unit =
    if (!expected.equals(found))
      fail(s"Expected $expected but found $found")

  def assert(value: Boolean, failMsg: String): Unit =
    if (!value) fail(failMsg)

  def pass(): Unit =
    println(StringUtils.indent(s"✅ $service $endpointName", 4))

  def fail(message: String): Nothing =
    throw new TestFailedException(StringUtils.indent(s"❌ $service $endpointName: $message", 4))
}
