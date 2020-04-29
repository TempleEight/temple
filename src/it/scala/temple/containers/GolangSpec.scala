package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import scalaj.http.Http
import temple.DSL.DSLProcessor
import temple.DSL.semantics.Analyzer
import temple.builder.project.ProjectBuilder
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.FileSystem._
import temple.utils.MonadUtils.FromEither
import temple.utils.StringUtils

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
        val marginWidth = 6 // width of margin for column numbers: ≤4 for line number, 1 for colon, ≥1 space
        for ((line, i) <- content.linesIterator.zipWithIndex) {
          System.err.println(s"${i + 1}:".padTo(marginWidth, ' ') + line)
        }
      }
    }
    errors
  }

  protected def buildAndValidate(templefile: String): Iterable[String] = {
    val parsed = DSLProcessor.parse(templefile) fromEither { error =>
      throw new RuntimeException(error)
    }
    val analyzedTemplefile = Analyzer.parseAndValidate(parsed)
    val detail             = GoLanguageDetail("example.com")
    val project            = ProjectBuilder.build(analyzedTemplefile, detail)

    analyzedTemplefile.allServices.keys.map(StringUtils.kebabCase).map { service =>
      val filesInService = project.files.filter { case (file, _) => file.folder.startsWith(service) }
      val validationErrors = validateAll(
        filesInService,
        entryFile = File(service, s"$service.go"),
      )

      validationErrors
    }
  }
}
