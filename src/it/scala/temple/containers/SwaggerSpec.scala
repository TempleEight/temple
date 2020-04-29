package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import org.scalatest.BeforeAndAfterAll
import scalaj.http.Http

abstract class SwaggerSpec extends DockerShell2HttpService(8082) with DockerTestKit with BeforeAndAfterAll {
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  val valid = "openapi.yaml is valid\n"

  // Validate a given OpenAPI specification, returning the output of swagger-cli
  def validate(openapi: String): String = {
    val json = Map("contents" -> openapi).asJson.toString()
    Http(swaggerVerifyUrl)
      .params(Map("openapi" -> json))
      .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000)
      .asString
      .body
  }
}
