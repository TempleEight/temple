package temple

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, Paths, SimpleFileVisitor}

import org.scalatest.{FlatSpec, Matchers}

class SimpleEndToEndTest extends FlatSpec with Matchers {
  behavior of "Temple"

  private def removeDirectory(path: Path): Unit =
    Files.walkFileTree(
      path,
      new SimpleFileVisitor[Path] {

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      },
    )

  it should "generate Postgres scripts" in {
    val basePath = Paths.get("/tmp/temple-e2e-test-1")
    removeDirectory(basePath)

    noException should be thrownBy Application.generate(
      new TempleConfig(
        Seq("generate", "-o", basePath.toAbsolutePath.toString, "src/e2e/scala/temple/testfiles/simple.temple"),
      ),
    )

    // Only one folder should have been generated
    Files.list(basePath).count() shouldBe 1
    Files.exists(basePath.resolve("user-db")) shouldBe true

    // Only one file should be present in the user-db folder
    Files.list(basePath.resolve("user-db")).count() shouldBe 1
    Files.exists(basePath.resolve("user-db").resolve("init.sql")) shouldBe true

    val initSql = Files.readString(basePath.resolve("user-db").resolve("init.sql"))
    initSql shouldBe SimpleEndToEndTestData.createStatement
  }
}
