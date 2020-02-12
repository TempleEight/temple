package temple.utils

import java.nio.file.FileAlreadyExistsException

import org.scalatest.{FlatSpec, Matchers}

class FileUtilsTest extends FlatSpec with Matchers {
  "Creating a folder that already exists" should "not throw an exception" in {
    FileUtils.createDirectory("src")
  }

  "Creating a folder where a file with that name already exists" should "throw an exception" in {
    a[FileAlreadyExistsException] should be thrownBy FileUtils.createDirectory("build.sbt")
  }
}
