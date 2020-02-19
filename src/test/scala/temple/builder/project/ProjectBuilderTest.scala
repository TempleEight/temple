package temple.builder.project

import org.scalatest.{FlatSpec, Matchers}
import temple.builder.project.Project.File

class ProjectBuilderTest extends FlatSpec with Matchers {
  behavior of "ProjectBuilder"

  it should "correctly create a simple project using postgres as the default" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefile)
    project.databaseCreationScripts shouldBe Map(
      File("users-db", "init.sql") -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
    )
  }

  it should "use postgres when defined at the project level" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefilePostgresProject)
    project.databaseCreationScripts shouldBe Map(
      File("users-db", "init.sql") -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
    )
  }

  it should "use postgres when defined at the service level" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefilePostgresService)
    project.databaseCreationScripts shouldBe Map(
      File("users-db", "init.sql") -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
    )
  }

  it should "correctly create a complex service with nested struct" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.complexTemplefile)
    project.databaseCreationScripts shouldBe Map(
      File("complexusers-db", "init.sql") -> ProjectBuilderTestData.complexTemplefilePostgresCreateOutput,
    )
  }
}
