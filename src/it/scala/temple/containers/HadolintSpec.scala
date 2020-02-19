package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import scalaj.http.Http

trait HadolintSpec extends FlatSpec with DockerTestKit with DockerHadolintService with BeforeAndAfterAll {
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  // https://github.com/hadolint/hadolint#rules
  object Rules {
    val useLatest = "DL3007"
  }

  // Validate a given Dockerfile, returning the output of Hadolint
  def validate(dockerfile: String): String = {
    val json = Map("contents" -> dockerfile).asJson.toString()
    Http(DockerHadolintService.externalVerifyUrl).params(Map("dockerfile" -> json)).asString.body
  }

}
