package temple.test.internal

abstract class ServiceTest(service: String) {
  println(s"🧪 Testing $service service")

  def testEndpoint(endpointName: String)(testFn: EndpointTest => Unit): Unit = {
    val endpointTest = new EndpointTest(service, endpointName)
    testFn(endpointTest)
    // If we successfully get here then tests have not thrown, so we've passed
    endpointTest.pass()
  }
}
