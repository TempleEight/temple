package temple.test.internal

import temple.ast.ServiceBlock
import temple.builder.project.ProjectBuilder
import temple.generate.CRUD

class CRUDServiceTest(name: String, service: ServiceBlock, allServices: Map[String, ServiceBlock], baseURL: String)
    extends ServiceTest(name) {

  private def testCreateEndpoint(accessToken: String): Unit =
    testEndpoint("create") { test =>
      val requestBody =
        ServiceTestUtils.constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      val createJSON = ServiceTestUtils
        .postRequest(
          test,
          s"http://$baseURL/api/${name.toLowerCase}",
          requestBody,
          accessToken,
        )
      test.validateResponseBody(
        requestBody.asObject.getOrElse(test.fail(s"Request was not a JSON object")),
        createJSON,
        service.attributes,
      )
    }

  // Test each type of endpoint that is present in the service
  def test(): Unit = {
    val accessToken = ServiceTestUtils.getAuthTokenWithEmail(name, baseURL)
    val endpoints   = ProjectBuilder.endpoints(service)
    if (endpoints.contains(CRUD.Create)) testCreateEndpoint(accessToken)
    // TODO: Read, Update, Delete, List
  }
}

object CRUDServiceTest {

  def test(name: String, service: ServiceBlock, allServices: Map[String, ServiceBlock], baseURL: String): Unit =
    new CRUDServiceTest(name, service, allServices, baseURL).test()
}
