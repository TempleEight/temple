package temple.builder.project

import java.nio.file.Paths

import org.scalatest.FlatSpec
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.FileMatchers
import temple.generate.FileSystem.File
import temple.utils.FileUtils

class ProjectBuilderTest extends FlatSpec with FileMatchers {

  // In order to update a test because you believe what the generator is outputting is entirely correct, call
  // temple.utils.FileUtils.outputProject(src/test/resources/my-project, actual)
  // and then check the diff on that folder to ensure it looks correct

  behavior of "ProjectBuilder"

  it should "correctly create a simple project using postgres as the default" in {
    val expected = FileUtils.buildFileMap(Paths.get("src/test/resources/project-builder-simple"))
    val actual =
      ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefile, GoLanguageDetail("github.com/squat/and/dab"))
    projectFilesShouldMatch(actual, expected)
  }

  it should "use postgres when defined at the project level" in {
    val expected = FileUtils.buildFileMap(Paths.get("src/test/resources/project-builder-simple"))
    val actual = ProjectBuilder
      .build(
        ProjectBuilderTestData.simpleTemplefilePostgresProject,
        GoLanguageDetail("github.com/squat/and/dab"),
      )
    projectFilesShouldMatch(actual, expected)
  }

  it should "use postgres when defined at the service level" in {
    val expected = FileUtils.buildFileMap(Paths.get("src/test/resources/project-builder-simple"))
    val actual = ProjectBuilder
      .build(ProjectBuilderTestData.simpleTemplefilePostgresService, GoLanguageDetail("github.com/squat/and/dab"))
//    temple.utils.FileUtils.outputProject("src/test/resources/project-builder-simple", actual)
    projectFilesShouldMatch(actual, expected)
  }

  it should "correctly generate config.json for cross service communication" in {
    val project = ProjectBuilder
      .build(ProjectBuilderTestData.simpleTemplefileForeignKeyService, GoLanguageDetail("github.com/squat/and/dab"))
    val actualConfig   = project.files(File("a", "config.json"))
    val expectedConfig = ProjectBuilderTestData.foreignKeyConfigJson
    actualConfig shouldBe expectedConfig
  }

  it should "correctly create a complex service with nested struct" in {
    val expected = FileUtils.buildFileMap(Paths.get("src/test/resources/project-builder-complex"))
    val actual =
      ProjectBuilder.build(ProjectBuilderTestData.complexTemplefile, GoLanguageDetail("github.com/squat/and/dab"))
  }
}
