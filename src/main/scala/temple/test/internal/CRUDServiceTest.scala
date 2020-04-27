package temple.test.internal

import scalaj.http.Http
import temple.ast.AbstractServiceBlock.ServiceBlock
import temple.test.internal.ServiceTestUtils._
import temple.ast.{Metadata, StructBlock}
import temple.builder.project.ProjectBuilder
import temple.generate.CRUD
import temple.utils.StringUtils.kebabCase

class CRUDServiceTest(
  serviceName: String,
  service: ServiceBlock,
  allServices: Map[String, ServiceBlock],
  baseURL: String,
  usesAuth: Boolean,
) extends ServiceTest(serviceName, baseURL, usesAuth) {

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

  def testIdentifyEndpoint(): Unit =
    testEndpoint("identify") { (test, accessToken) =>
      val requestBody = constructRequestBody(test, service.attributes, allServices, baseURL, accessToken)
      // Check the entity does not alreayd exist
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

  def testCreateStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("create", Some(structName)) { (test, accessToken) =>
      val parentID    = create(test, serviceName, allServices, baseURL, accessToken)
      val requestBody = constructRequestBody(test, struct.attributes, allServices, baseURL, accessToken)
      val createJSON  = postRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}", requestBody, accessToken)
      test.validateResponseBody(requestBody.asObject, createJSON, struct.attributes)
    }

  def testReadStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("read", Some(structName)) { (test, accessToken) =>
      val parentID = create(test, serviceName, allServices, baseURL, accessToken)
      val structID = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      val getJSON  = getRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", accessToken)
      test.validateResponseBody(None, getJSON, struct.attributes)
    }

  def testUpdateStructEndpoint(structName: String, struct: StructBlock): Unit =
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

  def testDeleteStructEndpoint(structName: String, struct: StructBlock): Unit =
    testEndpoint("delete", Some(structName)) { (test, accessToken) =>
      val parentID   = create(test, serviceName, allServices, baseURL, accessToken)
      val structID   = createStruct(test, parentID, structName, serviceName, allServices, baseURL, accessToken)
      val deleteJSON = deleteRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", accessToken)
      test.assert(deleteJSON.isEmpty, "delete response was not empty")

      // A subsequent GET should fail
      val code = executeRequest(test, s"$serviceURL/$parentID/${kebabCase(structName)}/$structID", accessToken)
      test.assertEqual(404, code, "entity should have been deleted")
    }

  def testListStructEndpoint(structName: String, struct: StructBlock): Unit =
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

  // Test each type of endpoint that is present in the service
  def test(): Boolean = {
    ProjectBuilder.endpoints(service).foreach {
      case CRUD.List     => testListEndpoint()
      case CRUD.Create   => testCreateEndpoint()
      case CRUD.Read     => testReadEndpoint()
      case CRUD.Update   => testUpdateEndpoint()
      case CRUD.Delete   => testDeleteEndpoint()
      case CRUD.Identify => testIdentifyEndpoint()
    }

    service.structs.foreach {
      case (name, struct) =>
        ProjectBuilder.endpoints(struct).foreach {
          case CRUD.List     => testListStructEndpoint(name, struct)
          case CRUD.Create   => testCreateStructEndpoint(name, struct)
          case CRUD.Read     => testReadStructEndpoint(name, struct)
          case CRUD.Update   => testUpdateStructEndpoint(name, struct)
          case CRUD.Delete   => testDeleteStructEndpoint(name, struct)
          case CRUD.Identify => throw new RuntimeException("A struct cannot have an identify endpoint")
        }
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
