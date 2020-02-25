package temple.builder.project

import org.scalatest.{FlatSpec, Matchers}
import temple.builder.project.Project.File

class ProjectBuilderTest extends FlatSpec with Matchers {

  behavior of "ProjectBuilder"

  it should "correctly create a simple project using postgres as the default" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefile)
    project.files shouldBe Map(
      File("templeuser-db", "init.sql") -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
      File("templeuser", "Dockerfile")  -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
    )
  }

  it should "use postgres when defined at the project level" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefilePostgresProject)
    project.files shouldBe Map(
      File("templeuser-db", "init.sql") -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
      File("templeuser", "Dockerfile")  -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
    )
  }

  it should "use postgres when defined at the service level" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefilePostgresService)
    project.files shouldBe Map(
      File("templeuser-db", "init.sql") -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
      File("templeuser", "Dockerfile")  -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
    )
  }

  it should "correctly create a complex service with nested struct" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.complexTemplefile)
    project.files shouldBe Map(
      File("complexuser-db", "init.sql") -> ProjectBuilderTestData.complexTemplefilePostgresCreateOutput,
      File("complexuser", "Dockerfile")  -> ProjectBuilderTestData.complexTemplefileUsersDockerfile,
    )
  }
}
