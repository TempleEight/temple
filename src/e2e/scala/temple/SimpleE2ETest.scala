package temple

import java.nio.file.{Files, Paths}

import org.scalatest.FlatSpec
import temple.detail.PoliceSergeantNicholasAngel
import temple.generate.FileMatchers
import temple.utils.FileUtils

import scala.reflect.io.Directory

class SimpleE2ETest extends FlatSpec with FileMatchers {

  behavior of "Temple"

  it should "generate the full project for simple.temple" in {
    val basePath = Files.createTempDirectory("simple-temple")

    // Generate from simple.temple
    noException should be thrownBy Application.generate(
      new TempleConfig(
        Seq("generate", "-o", basePath.toAbsolutePath.toString, "src/test/scala/temple/testfiles/simple.temple"),
      ),
      PoliceSergeantNicholasAngel,
    )

    val expected = FileUtils.buildFileMap(Paths.get("src/e2e/resources/simple-temple-expected"))
    val found    = FileUtils.buildFileMap(basePath)
    filesShouldMatch(found, expected)
  }

  it should "generate a full project with all possible attributes" in {
    val basePath = Files.createTempDirectory("attributes")

    // Generate from attributes.temple
    noException should be thrownBy Application.generate(
      new TempleConfig(
        Seq("generate", "-o", basePath.toAbsolutePath.toString, "src/test/scala/temple/testfiles/attributes.temple"),
      ),
      PoliceSergeantNicholasAngel,
    )

    val expected = FileUtils.buildFileMap(Paths.get("src/e2e/resources/attributes-temple-expected"))
    val found    = FileUtils.buildFileMap(basePath)
    filesShouldMatch(found, expected)
  }
}
