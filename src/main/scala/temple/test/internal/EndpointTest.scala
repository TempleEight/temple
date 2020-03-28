package temple.test.internal

private[internal] class EndpointTest(service: String, endpointName: String) {
  class TestFailedException(msg: String) extends RuntimeException(msg)

  def assertEqual(expected: Any, found: Any): Unit =
    if (!expected.equals(found))
      fail(s"Expected $expected but found $found")

  def assert(value: Boolean, failMsg: String): Unit =
    if (!value) fail(failMsg)

  def pass(): Unit =
    println(s"\t✅ $service $endpointName")

  def fail(message: String): Nothing =
    throw new TestFailedException(s"\t❌ $service $endpointName: $message")
}
