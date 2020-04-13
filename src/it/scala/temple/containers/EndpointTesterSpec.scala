package temple.containers

import java.nio.file.Files

import org.scalatest.FlatSpec
import temple.DSL.DSLProcessor
import temple.DSL.semantics.Analyzer
import temple.builder.project.ProjectBuilder
import temple.detail.LanguageSpecificDetailBuilder
import temple.test.ProjectTester
import temple.utils.FileUtils
import temple.utils.MonadUtils.FromEither

abstract class EndpointTesterSpec extends FlatSpec {

  def testEndpoints(templefile: String): Unit = {
    // Parse and validate templefile
    val data = DSLProcessor.parse(templefile) fromEither { error =>
      throw new RuntimeException(error)
    }
    val analyzedTemplefile = Analyzer.parseAndValidate(data)

    // Build project
    val detail =
      LanguageSpecificDetailBuilder.build(analyzedTemplefile, (_: String) => "github.com/Temple/integration-test")
    val project = ProjectBuilder.build(analyzedTemplefile, detail)

    val directory = Files.createTempDirectory(analyzedTemplefile.projectName).toAbsolutePath.toString
    FileUtils.outputProject(directory, project)
    ProjectTester.test(analyzedTemplefile, directory)
  }
}
