package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import org.scalatest.BeforeAndAfterAll
import scalaj.http.Http
import temple.generate.FileSystem._

abstract class KubeSpec extends DockerShell2HttpService(8084) with DockerTestKit with BeforeAndAfterAll {
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  def validateAll(files: Files, entryFile: File): String = {
    val json = files.asJson.toString()
    Http(kubeVerifyUrl)
      .params(Map("src" -> json, "root" -> entryFile.folder))
      .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000)
      .asString
      .body
  }
}
