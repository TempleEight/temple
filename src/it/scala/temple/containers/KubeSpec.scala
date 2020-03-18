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

  def validateAll(files: Map[File, FileContent], entryFile: File): String = {
    val json = files.map { case (file, contents) => (file.folder + "/" + file.filename, contents) }.asJson.toString()
    Http(kubeVerifyUrl)
      .params(Map("src" -> json, "root" -> entryFile.folder))
      .asString
      .body
  }
}
