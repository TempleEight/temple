package temple.test.internal

import temple.ast.AbstractServiceBlock.ServiceBlock
import temple.ast.Metadata
import temple.builder.project.ProjectBuilder
import temple.generate.CRUD
import temple.utils.StringUtils

class CRUDServiceTest(
  name: String,
  service: ServiceBlock,
  allServices: Map[String, ServiceBlock],
  baseURL: String,
  usesAuth: Boolean,
) extends ServiceTest(name) {

  private def newToken(): String = if (usesAuth) ServiceTestUtils.getAuthTokenWithEmail(name, baseURL) else ""

  private def testCreateEndpoint(): Unit =
    testEndpoint("create") { test =>
      val accessToken = newToken()
      val requestBody =
        ServiceTestUtils.constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      val createJSON =
        ServiceTestUtils
          .postRequest(test, s"http://$baseURL/api/${StringUtils.kebabCase(name)}", requestBody, accessToken)
      test.validateResponseBody(requestBody.asObject, createJSON, service.attributes)
    }

  private def testReadEndpoint(): Unit =
    testEndpoint("read") { test =>
      val accessToken    = newToken()
      val createResponse = ServiceTestUtils.create(test, name, allServices, baseURL, accessToken)
      val getJSON = ServiceTestUtils
        .getRequest(
          test,
          s"http://$baseURL/api/${StringUtils.kebabCase(name)}/${createResponse}",
          accessToken,
        )
      test.validateResponseBody(None, getJSON, service.attributes)
    }

  private def testUpdateEndpoint(): Unit =
    testEndpoint("update") { test =>
      val accessToken    = newToken()
      val createResponse = ServiceTestUtils.create(test, name, allServices, baseURL, accessToken)
      val requestBody =
        ServiceTestUtils
          .constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      val updateJSON =
        ServiceTestUtils.putRequest(
          test,
          s"http://$baseURL/api/${StringUtils.kebabCase(name)}/${createResponse}",
          requestBody,
          accessToken,
        )
      test.validateResponseBody(requestBody.asObject, updateJSON, service.attributes)
    }

  private def testDeleteEndpoint(): Unit =
    testEndpoint("delete") { test =>
      val accessToken    = newToken()
      val createResponse = ServiceTestUtils.create(test, name, allServices, baseURL, accessToken)
      val deleteJSON =
        ServiceTestUtils.deleteRequest(
          test,
          s"http://$baseURL/api/${StringUtils.kebabCase(name)}/${createResponse}",
          accessToken,
        )
      test.assert(deleteJSON.isEmpty, "delete response was not empty")
    }

  private def testListEndpoint(): Unit =
    testEndpoint("list") { test =>
      val accessToken = newToken()
      val _           = ServiceTestUtils.create(test, name, allServices, baseURL, accessToken)
      val listJSON =
        ServiceTestUtils
          .getRequest(test, s"http://$baseURL/api/${StringUtils.kebabCase(name)}/all", accessToken)
          .apply(s"${name}List")
          .flatMap(_.asArray)
          .getOrElse(test.fail(s"response did not contain key ${name}List"))

      // Ensure the correct number of items were returned
      // For `this`, only 1 item has been created for this access token...
      if (service.lookupMetadata[Metadata.Readable].contains(Metadata.Readable.This)) {
        test.assertEqual(1, listJSON.size, "expected list response to contain 1 item only")
      } else {
        test.assert(listJSON.nonEmpty, "expected list response to contain at least 1 item")
      }

      listJSON.foreach { listItem =>
        val listObject = listItem.asObject.getOrElse(test.fail("list item was not a JSON object"))
        test.validateResponseBody(None, listObject, service.attributes)
      }
    }

  // Test each type of endpoint that is present in the service
  def test(): Boolean = {
    ProjectBuilder.endpoints(service).foreach {
      case CRUD.List     => testListEndpoint()
      case CRUD.Create   => testCreateEndpoint()
      case CRUD.Read     => testReadEndpoint()
      case CRUD.Update   => testUpdateEndpoint()
      case CRUD.Delete   => testDeleteEndpoint()
      case CRUD.Identify => // TODO
    }
    anyTestFailed
  }
}

object CRUDServiceTest {

  def test(
    name: String,
    service: ServiceBlock,
    allServices: Map[String, ServiceBlock],
    baseURL: String,
    usesAuth: Boolean,
  ): Boolean =
    new CRUDServiceTest(name, service, allServices, baseURL, usesAuth).test()
}
