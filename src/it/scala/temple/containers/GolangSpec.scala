package temple.containers

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import io.circe.syntax._
import org.scalatest.BeforeAndAfterAll
import scalaj.http.Http
import temple.DSL.DSLProcessor
import temple.DSL.semantics.Analyzer
import temple.builder.project.ProjectBuilder
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.FileSystem._
import temple.utils.StringUtils
import temple.utils.MonadUtils.FromEither

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
