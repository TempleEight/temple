package temple.utils

import java.nio.file.{FileAlreadyExistsException, Files, Paths}

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class FileUtilsTest extends FlatSpec with Matchers {

  def randomString(length: Int): String = new Random().alphanumeric.take(length).mkString

  "Creating a folder that already exists" should "not throw an exception" in {
    FileUtils.createDirectory("src")
    noException should be thrownBy FileUtils.createDirectory("src")
  }

  "Creating a folder where a file with that name already exists" should "throw an exception" in {
    a[FileAlreadyExistsException] should be thrownBy FileUtils.createDirectory("build.sbt")
  }

  "Creating a file" should "succeed" in {
    // Generate a filename randomly, retrying if the file name already exists
    val filename = Iterator.continually(s"/tmp/test-${randomString(10)}").find(x => !Files.exists(Paths.get(x))).get

    val fileContents = "Example file contents"
    FileUtils.writeToFile(filename, fileContents)
    FileUtils.readFile(filename) shouldBe fileContents
  }
}
