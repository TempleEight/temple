package temple

import java.nio.file.{Files, Paths}

import org.scalatest.{FlatSpec, Matchers}

import scala.jdk.StreamConverters._
import scala.reflect.io.Directory

class SimpleE2ETest extends FlatSpec with Matchers {

  behavior of "Temple"

  it should "generate Postgres scripts" in {
    // Clean up folder if exists
    val basePath  = Paths.get("/tmp/temple-e2e-test-1")
    val directory = new Directory(basePath.toFile)
    directory.deleteRecursively()

    noException should be thrownBy Application.generate(
      new TempleConfig(
        Seq("generate", "-o", basePath.toAbsolutePath.toString, "src/test/scala/temple/testfiles/simple.temple"),
      ),
    )

    // Four folders should have been generated
    val expectedFolders = Set("templeuser-db", "templeuser", "kong", "kube").map(dir => basePath.resolve(dir))
    Files.list(basePath).toScala(Set) shouldBe expectedFolders

    // Only one file should be present in the templeuser-db folder
    val expectedTempleUserDbFiles = Set("init.sql").map(dir => basePath.resolve("templeuser-db").resolve(dir))
    Files.list(basePath.resolve("templeuser-db")).toScala(Set) shouldBe expectedTempleUserDbFiles

    // The content of the templeuser-db/init.sql file should be correct
    val initSql = Files.readString(basePath.resolve("templeuser-db").resolve("init.sql"))
    initSql shouldBe SimpleE2ETestData.createStatement

    // Only one file should be present in the templeuser folder
    val expectedTempleUserFiles = Set("Dockerfile").map(dir => basePath.resolve("templeuser").resolve(dir))
    Files.list(basePath.resolve("templeuser")).toScala(Set) shouldBe expectedTempleUserFiles

    // The content of the templeuser/Dockerfile file should be correct
    val templeUserDockerfile = Files.readString(basePath.resolve("templeuser").resolve("Dockerfile"))
    templeUserDockerfile shouldBe SimpleE2ETestData.dockerfile
  }
}
