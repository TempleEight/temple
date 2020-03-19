package temple.builder.project

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.FileSystem._

class ProjectBuilderTest extends FlatSpec with Matchers {

  behavior of "ProjectBuilder"

  it should "correctly create a simple project using postgres as the default" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefile)
    val expected = Map(
        File("templeuser-db", "init.sql")             -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("templeuser", "Dockerfile")              -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("kube/TempleUser", "deployment.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/TempleUser", "db-deployment.yaml") -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/TempleUser", "service.yaml")       -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/TempleUser", "db-service.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/TempleUser", "db-storage.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")             -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
      ) ++ ProjectBuilderTestData.kongFiles
    project.files shouldBe expected
  }

  it should "use postgres when defined at the project level" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefilePostgresProject)
    val expected = Map(
        File("templeuser-db", "init.sql")             -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("templeuser", "Dockerfile")              -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("kube/TempleUser", "deployment.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/TempleUser", "db-deployment.yaml") -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/TempleUser", "service.yaml")       -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/TempleUser", "db-service.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/TempleUser", "db-storage.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")             -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
      ) ++ ProjectBuilderTestData.kongFiles
    project.files shouldBe expected
  }

  it should "use postgres when defined at the service level" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefilePostgresService)
    val expected = Map(
        File("templeuser-db", "init.sql")             -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("templeuser", "Dockerfile")              -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("kube/TempleUser", "deployment.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/TempleUser", "db-deployment.yaml") -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/TempleUser", "service.yaml")       -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/TempleUser", "db-service.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/TempleUser", "db-storage.yaml")    -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")             -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
      ) ++ ProjectBuilderTestData.kongFiles
    project.files shouldBe expected
  }

  it should "correctly create a complex service with nested struct" in {
    val project = ProjectBuilder.build(ProjectBuilderTestData.complexTemplefile)
    project.files shouldBe Map(
      File("complexuser-db", "init.sql")             -> ProjectBuilderTestData.complexTemplefilePostgresCreateOutput,
      File("complexuser", "Dockerfile")              -> ProjectBuilderTestData.complexTemplefileUsersDockerfile,
      File("kube/ComplexUser", "deployment.yaml")    -> ProjectBuilderTestData.complexTemplefileKubeDeployment,
      File("kube/ComplexUser", "db-deployment.yaml") -> ProjectBuilderTestData.complexTemplefileKubeDbDeployment,
      File("kube/ComplexUser", "service.yaml")       -> ProjectBuilderTestData.complexTemplefileKubeService,
      File("kube/ComplexUser", "db-service.yaml")    -> ProjectBuilderTestData.complexTemplefileKubeDbService,
      File("kube/ComplexUser", "db-storage.yaml")    -> ProjectBuilderTestData.complexTemplefileKubeDbStorage,
      File("kong", "configure-kong.sh")              -> ProjectBuilderTestData.complexTemplefileConfigureKong,
    ) ++ ProjectBuilderTestData.kongFiles
  }
}
