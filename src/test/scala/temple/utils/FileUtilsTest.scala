package temple.utils

import java.nio.file.{FileAlreadyExistsException, Files, Paths}

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.FileSystem.File

class FileUtilsTest extends FlatSpec with Matchers {

  behavior of "FileUtils"

  it should "not throw an exception when creating a folder that already exists" in {
    FileUtils.createDirectory("src")
    noException should be thrownBy FileUtils.createDirectory("src")
  }

  it should "throw an exception when creating a folder where a file with that name already exists" in {
    a[FileAlreadyExistsException] should be thrownBy FileUtils.createDirectory("build.sbt")
  }

  it should "create a file successfully in" in {
    // Generate a filename randomly, retrying if the file name already exists
    val filename =
      Iterator.continually(s"/tmp/test-${StringUtils.randomString(10)}").find(x => !Files.exists(Paths.get(x))).get

    val fileContents = "Example file contents"
    FileUtils.writeToFile(filename, fileContents)
    FileUtils.readFile(filename) shouldBe fileContents
  }

  it should "read a binary file successfully" in {
    val expectedDogSize = 128166
    val dog             = FileUtils.readBinaryFile("src/test/scala/temple/testfiles/dog.jpeg")
    dog.length shouldBe expectedDogSize
  }

  it should "correctly get all the filenames under a directory" in {
    val foundFiles = FileUtils.buildFilenameSeq(Paths.get("src/test/scala/temple/testfiles"))
    foundFiles.toSet shouldBe Set(
      File("", "attributes.temple"),
      File("", "badSemantics.temple"),
      File("", "badSyntax.temple"),
      File("", "badValidation.temple"),
      File("", "data.temple"),
      File("", "dog.jpeg"),
      File("", "enumReadAll.temple"),
      File("", "simple.temple"),
      File("", "simple-dc.temple"),
    )
  }
}
