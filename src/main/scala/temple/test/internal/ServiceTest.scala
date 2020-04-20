package temple.test.internal

import temple.utils.StringUtils

abstract class ServiceTest(service: String, baseURL: String, usesAuth: Boolean) {
  protected var anyTestFailed = false
  protected val serviceURL    = s"http://$baseURL/api/${StringUtils.kebabCase(service)}"
  println(s"🧪 Testing $service service")

  def testEndpoint(endpointName: String)(testFn: (EndpointTest, String) => Unit): Unit = {
    val endpointTest = new EndpointTest(service, endpointName)
    try {
      val token = if (usesAuth) ServiceTestUtils.getAuthTokenWithEmail(service, baseURL) else ""
      testFn(endpointTest, token)
      // If we successfully get here then tests have not thrown, so we've passed
      endpointTest.pass()
    } catch {
      case e: TestFailedException =>
        anyTestFailed = true
        println(e.getMessage)
    }
  }
}
