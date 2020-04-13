package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import org.scalatest.BeforeAndAfterAll
import scalaj.http.Http
import temple.generate.FileSystem._

abstract class GolangSpec extends DockerShell2HttpService(8081) with DockerTestKit with BeforeAndAfterAll {
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  // Validate a given Go file, returning the output of the Go compiler
  def validate(gofile: String): String = {
    val file = File("sampleDir", "main.go")
    validateAll(Map(file -> gofile), file)
  }

  def validateAll(files: Files, entryFile: File): String = {
    val json = files.map { case (file, contents) => (file.folder + "/" + file.filename, contents) }.asJson.toString()
    Http(golangVerifyUrl)
      .params(Map("src" -> json, "root" -> entryFile.folder, "entrypoint" -> entryFile.filename))
      .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000)
      .asString
      .body
  }
}
