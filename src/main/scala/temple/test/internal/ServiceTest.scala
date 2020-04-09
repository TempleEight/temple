package temple.test.internal

abstract class ServiceTest(service: String) {
  protected var anyTestFailed = false
  println(s"🧪 Testing $service service")

  def testEndpoint(endpointName: String)(testFn: EndpointTest => Unit): Unit = {
    val endpointTest = new EndpointTest(service, endpointName)
    try {
      testFn(endpointTest)
      // If we successfully get here then tests have not thrown, so we've passed
      endpointTest.pass()
    } catch {
      case e: TestFailedException =>
        anyTestFailed = true
        println(e.getMessage)
    }
  }
}
