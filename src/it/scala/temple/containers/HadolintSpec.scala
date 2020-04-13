package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import org.scalatest.BeforeAndAfterAll
import scalaj.http.Http

abstract class HadolintSpec extends DockerShell2HttpService(8080) with DockerTestKit with BeforeAndAfterAll {
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  // https://github.com/hadolint/hadolint#rules
  object Rules {
    val useLatest: String = "DL3007"
  }

  // Validate a given Dockerfile, returning the output of Hadolint
  def validate(dockerfile: String): String = {
    val json = Map("contents" -> dockerfile).asJson.toString()
    Http(hadolintVerifyUrl)
      .params(Map("dockerfile" -> json))
      .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000)
      .asString
      .body
  }
}
