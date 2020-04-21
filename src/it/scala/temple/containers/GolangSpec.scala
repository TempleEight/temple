package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import scalaj.http.Http
import temple.generate.FileSystem._

import scala.collection.immutable.SortedMap

abstract class GolangSpec extends DockerShell2HttpService(8081) with DockerTestKit {
  implicit override val dockerFactory: DockerFactory = new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  // Validate a given Go file, returning the output of the Go compiler
  def validate(gofile: String): String = {
    val file = File("sampleDir", "main.go")
    validateAll(Map(file -> gofile), file)
  }

  def validateAll(files: Files, entryFile: File): String = {
    val json = files.asJson.toString()
    val errors = Http(golangVerifyUrl)
      .params(Map("src" -> json, "root" -> entryFile.folder, "entrypoint" -> entryFile.filename))
      .timeout(connTimeoutMs = 1000, readTimeoutMs = 60000)
      .asString
      .body
    if (errors.nonEmpty) {
      for ((file, content) <- files.to(SortedMap)) {
        System.err.println(s"$file:")
        for ((line, i) <- content.linesIterator.zipWithIndex) {
          System.err.println(s"${i + 1}:".padTo(6, ' ') + line)
        }
      }
    }
    errors
  }
}
