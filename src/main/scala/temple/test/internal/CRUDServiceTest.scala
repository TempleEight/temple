package temple.test.internal

import java.net.HttpURLConnection

import scalaj.http.Http
import temple.ast.AbstractServiceBlock.ServiceBlock
import temple.ast.Metadata.{Readable, Writable}
import temple.test.internal.ServiceTestUtils._
import temple.ast.{AbstractAttribute, Metadata, StructBlock}
import temple.builder.project.ProjectBuilder
import temple.generate.CRUD
import temple.generate.CRUD.CRUD
import temple.utils.StringUtils.kebabCase
import temple.builder.project.ProjectConfig.{getDefaultReadable, getDefaultWritable}

class CRUDServiceTest(
  serviceName: String,
  service: ServiceBlock,
  allServices: Map[String, ServiceBlock],
  baseURL: String,
  usesAuth: Boolean,
) extends ServiceTest(serviceName, baseURL, usesAuth) {

  private def serviceReadable: Metadata.Readable =
    service.lookupLocalMetadata[Metadata.Readable].getOrElse(getDefaultReadable(usesAuth))

  private def serviceWritable: Metadata.Writable =
    service.lookupLocalMetadata[Metadata.Writable].getOrElse(getDefaultWritable(usesAuth))

  private def testReadability(test: EndpointTest, endpoints: Set[CRUD], url: String): Unit = {
    val accessToken = if (usesAuth) getAuthTokenWithEmail(serviceName, baseURL) else ""
    val expected = serviceReadable match {
      case Readable.All  => HttpURLConnection.HTTP_OK
      case Readable.This => HttpURLConnection.HTTP_UNAUTHORIZED
    }

    if (endpoints.contains(CRUD.Read)) {
      // Make a read request using a new access token, then validate the response code
      val responseCode = executeRequest(test, url, accessToken)
      test.assertEqual(expected, responseCode, "entity did not have the correct readability")
    }
  }

  private def testWritability(
    test: EndpointTest,
    attributes: Map[String, AbstractAttribute],
    endpoints: Set[CRUD],
    url: String,
  ): Unit = {
    val accessToken = if (usesAuth) getAuthTokenWithEmail(serviceName, baseURL) else ""
    val expected = serviceWritable match {
      case Writable.All  => HttpURLConnection.HTTP_OK
      case Writable.This => HttpURLConnection.HTTP_UNAUTHORIZED
    }

    if (endpoints.contains(CRUD.Update)) {
      // Make an update request using a new access token, then validate the response code
      val requestBody    = constructRequestBody(test, attributes, allServices, baseURL, accessToken)
      val updateResponse = executeRequest(test, url, accessToken, "PUT", Some(requestBody))
      test.assertEqual(expected, updateResponse, "entity did not have the correct writability for updating")
    }

    if (endpoints.contains(CRUD.Delete)) {
      // Also attempt to make a delete call - the outcome should be the same
      val deleteResponse = executeRequest(test, url, accessToken, "DELETE")
      test.assertEqual(expected, deleteResponse, "entity did not have the correct writability for deleting")
    }
  }

  private def testCreateEndpoint(): Unit =
    testEndpoint("create") { (test, accessToken) =>
      val requestBody = constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      val createJSON  = postRequest(test, serviceURL, requestBody, accessToken)
      test.validateResponseBody(requestBody.asObject, createJSON, service.attributes)
    }

  private def testReadEndpoint(): Unit =
    testEndpoint("read") { (test, accessToken) =>
      val id      = create(test, serviceName, allServices, baseURL, accessToken)
      val getJSON = getRequest(test, s"$serviceURL/$id", accessToken)
      test.validateResponseBody(None, getJSON, service.attributes)
    }

  private def testUpdateEndpoint(): Unit =
    testEndpoint("update") { (test, accessToken) =>
      val id          = create(test, serviceName, allServices, baseURL, accessToken)
      val requestBody = constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      val updateJSON  = putRequest(test, s"$serviceURL/$id", requestBody, accessToken)
      test.validateResponseBody(requestBody.asObject, updateJSON, service.attributes)
    }

  private def testDeleteEndpoint(): Unit =
    testEndpoint("delete") { (test, accessToken) =>
      val id         = create(test, serviceName, allServices, baseURL, accessToken)
      val deleteJSON = deleteRequest(test, s"$serviceURL/$id", accessToken)
      test.assert(deleteJSON.isEmpty, "delete response was not empty")
    }

  private def testListEndpoint(): Unit =
    testEndpoint("list") { (test, accessToken) =>
      val _ = create(test, serviceName, allServices, baseURL, accessToken)
      val listJSON = getRequest(test, s"$serviceURL/all", accessToken)
        .apply(s"${serviceName}List")
        .flatMap(_.asArray)
        .getOrElse(test.fail(s"response did not contain key ${serviceName}List"))

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

  private def testIdentifyEndpoint(): Unit =
    testEndpoint("identify") { (test, accessToken) =>
      val requestBody = constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      // Check the entity does not already exist
      val preIdentifyResponse = Http(serviceURL).method("GET").header("Authorization", s"Bearer $accessToken").asString
      test.assertEqual(404, preIdentifyResponse.code)

      // Construct an entity, but discard the ID
      val _ = postRequest(test, serviceURL, requestBody, accessToken)

      // Check the entity can be identified from the access token
      val identifyResponse = Http(serviceURL).method("GET").header("Authorization", s"Bearer $accessToken").asString
      test.assertEqual(302, identifyResponse.code)

      val location =
        identifyResponse.header("Location").getOrElse(test.fail("response did not contain a Location header"))

      // Get the result of the identification
      val response = ServiceTestUtils.getRequest(test, location, accessToken)

      // Validate this is exactly what was created at the start
      test.validateResponseBody(requestBody.asObject, response, service.attributes)
    }

  private def testCreateStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("create", Some(structName)) { (test, accessToken) =>
      val parentID    = create(test, serviceName, allServices, baseURL, accessToken)
      val requestBody = constructRequestBody(test, struct.attributes, allServices, baseURL, accessToken)
      val createJSON  = postRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}", requestBody, accessToken)
      test.validateResponseBody(requestBody.asObject, createJSON, struct.attributes)
    }

  private def testReadStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("read", Some(structName)) { (test, accessToken) =>
      val parentID = create(test, serviceName, allServices, baseURL, accessToken)
      val structID = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      val getJSON  = getRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", accessToken)
      test.validateResponseBody(None, getJSON, struct.attributes)
    }

  private def testUpdateStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("update", Some(structName)) { (test, accessToken) =>
      val parentID    = create(test, serviceName, allServices, baseURL, accessToken)
      val structID    = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      val requestBody = constructRequestBody(test, struct.attributes, allServices, baseURL, accessToken)
      val updateJSON =
        putRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", requestBody, accessToken)
      test.validateResponseBody(requestBody.asObject, updateJSON, struct.attributes)

      // A subsequent GET should match the updated values
      val getJSON = getRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", accessToken)
      test.validateResponseBody(requestBody.asObject, getJSON, struct.attributes)
    }

  private def testDeleteStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("delete", Some(structName)) { (test, accessToken) =>
      val parentID   = create(test, serviceName, allServices, baseURL, accessToken)
      val structID   = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      val deleteJSON = deleteRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", accessToken)
      test.assert(deleteJSON.isEmpty, "delete response was not empty")

      // A subsequent GET should fail
      val code = executeRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", accessToken)
      test.assertEqual(404, code, "entity should have been deleted")
    }

  private def testListStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("list", Some(structName)) { (test, accessToken) =>
      val parentID = create(test, serviceName, allServices, baseURL, accessToken)
      val _        = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      val listJSON = getRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/all", accessToken)
        .apply(s"${structName}List")
        .flatMap(_.asArray)
        .getOrElse(test.fail(s"response did not contain key ${structName}List"))
      test.assertEqual(1, listJSON.size, "expected list response to contain 1 item only")

      listJSON.foreach { listItem =>
        val listObject = listItem.asObject.getOrElse(test.fail("list item was not a JSON object"))
        test.validateResponseBody(None, listObject, struct.attributes)
      }
    }

  // Determine that entities within the service can be correctly read
  private def testServiceReadability(endpoints: Set[CRUD]): Unit =
    testEndpoint("readability") { (test, accessToken) =>
      val id = create(test, serviceName, allServices, baseURL, accessToken)
      testReadability(test, endpoints, s"$serviceURL/$id")
    }

  // Determine that entities within the service can be correctly written
  private def testServiceWritability(endpoints: Set[CRUD]): Unit =
    testEndpoint("writablity") { (test, accessToken) =>
      val id = create(test, serviceName, allServices, baseURL, accessToken)
      testWritability(test, service.attributes, endpoints, s"$serviceURL/$id")
    }

  // Determine that entities within the service can be correctly read
  private def testStructReadability(structName: String, endpoints: Set[CRUD]): Unit =
    testEndpoint("readability", Some(structName)) { (test, accessToken) =>
      val parentID = create(test, serviceName, allServices, baseURL, accessToken)
      val structID = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      testReadability(test, endpoints, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID")
    }

  private def testStructWritability(structName: String, struct: StructBlock, endpoints: Set[CRUD.CRUD]): Unit =
    testEndpoint("writability", Some(structName)) { (test, accessToken) =>
      val parentID = create(test, serviceName, allServices, baseURL, accessToken)
      val structID = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      testWritability(test, struct.attributes, endpoints, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID")
    }

  // Test each type of endpoint that is present in the service
  def test(): Boolean = {
    val serviceEndpoints = ProjectBuilder.endpoints(service)
    serviceEndpoints.foreach {
      case CRUD.List     => testListEndpoint()
      case CRUD.Create   => testCreateEndpoint()
      case CRUD.Read     => testReadEndpoint()
      case CRUD.Update   => testUpdateEndpoint()
      case CRUD.Delete   => testDeleteEndpoint()
      case CRUD.Identify => testIdentifyEndpoint()
    }
    testServiceReadability(serviceEndpoints)
    testServiceWritability(serviceEndpoints)

    service.structs.foreach {
      case (name, struct) =>
        val structEndpoints = ProjectBuilder.endpoints(struct)
        structEndpoints.foreach {
          case CRUD.List     => testListStructEndpoint(name, struct)
          case CRUD.Create   => testCreateStructEndpoint(name, struct)
          case CRUD.Read     => testReadStructEndpoint(name, struct)
          case CRUD.Update   => testUpdateStructEndpoint(name, struct)
          case CRUD.Delete   => testDeleteStructEndpoint(name, struct)
          case CRUD.Identify => throw new RuntimeException("A struct cannot have an identify endpoint")
        }
        testStructReadability(name, structEndpoints)
        testStructWritability(name, struct, structEndpoints)
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
