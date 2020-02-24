package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import org.scalatest.BeforeAndAfterAll
import scalaj.http.Http

abstract class GolangSpec extends DockerShell2HttpService(8081) with DockerTestKit with BeforeAndAfterAll {
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  // Validate a given Go file, returning the output of the Go compiler
  def validate(gofile: String): String = {
    val json = Map("contents" -> gofile).asJson.toString()
    Http(golangVerifyUrl).params(Map("gofile" -> json)).asString.body
  }
}
