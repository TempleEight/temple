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

    // Only one folder should have been generated
    Files.list(basePath).count() shouldBe 1
    Files.exists(basePath.resolve("user-db")) shouldBe true

    // Only one file should be present in the user-db folder
    Files.list(basePath.resolve("user-db")).count() shouldBe 1
    Files.exists(basePath.resolve("user-db").resolve("init.sql")) shouldBe true

    val initSql = Files.readString(basePath.resolve("user-db").resolve("init.sql"))
    initSql shouldBe SimpleE2ETestData.createStatement
  }
}
