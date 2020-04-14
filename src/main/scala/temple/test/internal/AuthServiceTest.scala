package temple.test.internal

import temple.ast.Metadata.ServiceAuth
import temple.utils.StringUtils
import io.circe.syntax._

object AuthServiceTest extends ServiceTest("Auth") {

  private def testEmailAuth(baseURL: String): Unit = {
    val email    = ServiceTestUtils.randomEmail()
    val password = StringUtils.randomString(10)

    // Register endpoint
    testEndpoint("register") { test =>
      val registerJson = ServiceTestUtils
        .postRequest(
          test,
          s"http://$baseURL/api/auth/register",
          Map("email" -> email, "password" -> password).asJson,
        )
      test.assert(registerJson.contains("AccessToken"), "response didn't contain the key AccessToken")
    }

    // Login endpoint
    testEndpoint("login") { test =>
      val loginJson = ServiceTestUtils
        .postRequest(
          test,
          s"http://$baseURL/api/auth/login",
          Map("email" -> email, "password" -> password).asJson,
        )
      test.assert(loginJson.contains("AccessToken"), "response didn't contain the key AccessToken")
    }
  }

  // Test each type of auth that is present in the project
  // TODO: refactor for central auth method
  def test(auths: Set[ServiceAuth], baseURL: String): Boolean = {
    auths.foreach {
      case ServiceAuth.Email => testEmailAuth(baseURL)
    }
    anyTestFailed
  }
}
