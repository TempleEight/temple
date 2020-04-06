package temple.test.internal

import temple.ast.AbstractServiceBlock.ServiceBlock
import temple.builder.project.ProjectBuilder
import temple.generate.CRUD

class CRUDServiceTest(name: String, service: ServiceBlock, allServices: Map[String, ServiceBlock], baseURL: String)
    extends ServiceTest(name) {

  private def testCreateEndpoint(accessToken: String): Unit =
    testEndpoint("create") { test =>
      val requestBody =
        ServiceTestUtils.constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      val createJSON =
        ServiceTestUtils.postRequest(test, s"http://$baseURL/api/${name.toLowerCase}", requestBody, accessToken)
      test.validateResponseBody(requestBody.asObject, createJSON, service.attributes)
    }

  private def testReadEndpoint(accessToken: String): Unit =
    testEndpoint("read") { test =>
      val createResponse = ServiceTestUtils.create(test, name, allServices, baseURL, accessToken)
      val getJSON = ServiceTestUtils
        .getRequest(test, s"http://$baseURL/api/${name.toLowerCase}/${createResponse.id}", createResponse.accessToken)
      test.validateResponseBody(None, getJSON, service.attributes)
    }

  private def testUpdateEndpoint(accessToken: String): Unit =
    testEndpoint("update") { test =>
      val createResponse = ServiceTestUtils.create(test, name, allServices, baseURL, accessToken)
      val requestBody =
        ServiceTestUtils
          .constructRequestBody(test, service.attributes, allServices, baseURL, createResponse.accessToken)
      val updateJSON =
        ServiceTestUtils.putRequest(
          test,
          s"http://$baseURL/api/${name.toLowerCase}/${createResponse.id}",
          requestBody,
          createResponse.accessToken,
        )
      test.validateResponseBody(requestBody.asObject, updateJSON, service.attributes)
    }

  private def testDeleteEndpoint(accessToken: String): Unit =
    testEndpoint("delete") { test =>
      val createResponse = ServiceTestUtils.create(test, name, allServices, baseURL, accessToken)
      val deleteJSON =
        ServiceTestUtils.deleteRequest(
          test,
          s"http://$baseURL/api/${name.toLowerCase}/${createResponse.id}",
          createResponse.accessToken,
        )
      test.assert(deleteJSON.isEmpty, "delete response was not empty")
    }

  // Test each type of endpoint that is present in the service
  def test(): Unit = {
    val accessToken = ServiceTestUtils.getAuthTokenWithEmail(name, baseURL)
    ProjectBuilder.endpoints(service).foreach {
      case CRUD.List   => // TODO
      case CRUD.Create => testCreateEndpoint(accessToken)
      case CRUD.Read   => testReadEndpoint(accessToken)
      case CRUD.Update => testUpdateEndpoint(accessToken)
      case CRUD.Delete => testDeleteEndpoint(accessToken)
    }
  }
}

object CRUDServiceTest {

  def test(name: String, service: ServiceBlock, allServices: Map[String, ServiceBlock], baseURL: String): Unit =
    new CRUDServiceTest(name, service, allServices, baseURL).test()
}
