package temple

import java.nio.file.{Files, Paths}

import org.scalatest.{FlatSpec, Matchers}

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

    // Two folders should have been generated
    Files.list(basePath).count() shouldBe 2
    Files.exists(basePath.resolve("templeuser-db")) shouldBe true
    Files.exists(basePath.resolve("templeuser")) shouldBe true

    // Only one file should be present in the templeuser-db folder
    Files.list(basePath.resolve("templeuser-db")).count() shouldBe 1
    Files.exists(basePath.resolve("templeuser-db").resolve("init.sql")) shouldBe true

    // The content of the templeuser-db/init.sql file should be correct
    val initSql = Files.readString(basePath.resolve("templeuser-db").resolve("init.sql"))
    initSql shouldBe SimpleE2ETestData.createStatement

    // Only one file should be present in the templeuser folder
    Files.list(basePath.resolve("templeuser")).count() shouldBe 1
    Files.exists(basePath.resolve("templeuser").resolve("Dockerfile")) shouldBe true

    // The content of the templeuser/Dockerfile file should be correct
    val templeUserDockerfile = Files.readString(basePath.resolve("templeuser").resolve("Dockerfile"))
    templeUserDockerfile shouldBe SimpleE2ETestData.dockerfile
  }
}
