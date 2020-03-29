package temple.test.internal

import temple.utils.StringUtils

private[internal] class EndpointTest(service: String, endpointName: String) {
  class TestFailedException(msg: String) extends RuntimeException(msg)

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
