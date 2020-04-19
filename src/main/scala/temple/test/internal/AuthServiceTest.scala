package temple.test.internal

import io.circe.syntax._
import temple.ast.Metadata.AuthMethod
import temple.utils.StringUtils
import temple.test.internal.ServiceTestUtils._

class AuthServiceTest(baseURL: String) extends ServiceTest("Auth", baseURL, false) {

  private def testEmailAuth(): Unit = {
    val email       = ServiceTestUtils.randomEmail()
    val password    = StringUtils.randomString(10)
    val requestBody = Map("email" -> email, "password" -> password).asJson

    // Register endpoint
    testEndpoint("register") { (test, _) =>
      val registerJson = postRequest(test, s"$serviceURL/register", requestBody)
      test.assert(registerJson.contains("AccessToken"), "response didn't contain the key AccessToken")
    }

    // Login endpoint
    testEndpoint("login") { (test, _) =>
      val loginJson = postRequest(test, s"$serviceURL/login", requestBody)
      test.assert(loginJson.contains("AccessToken"), "response didn't contain the key AccessToken")
    }
  }

  // Test each type of auth that is present in the project
  def test(authMethod: AuthMethod): Boolean = {
    authMethod match {
      case AuthMethod.Email => testEmailAuth()
    }
    anyTestFailed
  }
}

object AuthServiceTest {
  def test(authMethod: AuthMethod, baseURL: String): Boolean = new AuthServiceTest(baseURL).test(authMethod)
}
