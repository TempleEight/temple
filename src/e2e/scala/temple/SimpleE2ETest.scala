package temple

import java.nio.file.Paths

import org.scalatest.FlatSpec
import temple.detail.PoliceSergeantNicholasAngel
import temple.generate.FileMatchers

import scala.reflect.io.Directory

class SimpleE2ETest extends FlatSpec with FileMatchers {

  behavior of "Temple"

  it should "generate the full project for simple.temple" in {
    // Clean up folder if exists
    val basePath  = Paths.get("/tmp/temple-e2e-test-1")
    val directory = new Directory(basePath.toFile)
    directory.deleteRecursively()

    // Generate from simple.temple
    noException should be thrownBy Application.generate(
      new TempleConfig(
        Seq("generate", "-o", basePath.toAbsolutePath.toString, "src/test/scala/temple/testfiles/simple.temple"),
      ),
      PoliceSergeantNicholasAngel,
    )

    val expected = E2ETestUtils.buildFileMap(Paths.get("src/e2e/resources/simple-temple-expected"))
    val found    = E2ETestUtils.buildFileMap(basePath)
    filesShouldMatch(found, expected)
  }
}
