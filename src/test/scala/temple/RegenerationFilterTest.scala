package temple

import java.nio.file.{Files, Path}

import org.scalatest.{BeforeAndAfter, FlatSpec}
import temple.detail.PoliceSergeantNicholasAngel
import temple.generate.{FileMatchers, FileSystem}
import temple.utils.FileUtils

import scala.reflect.io.Directory

class RegenerationFilterTest extends FlatSpec with FileMatchers with BeforeAndAfter {

  behavior of "RegenerationFilterTest"

  private var testDirectory: Path = _

  before {
    testDirectory = Files.createTempDirectory("test")
  }

  after {
    if (testDirectory.toFile.exists()) new Directory(testDirectory.toFile).deleteRecursively()
  }

  private def setupFileSystemWhole(): Unit = {
    FileUtils.writeToFile(testDirectory.toString + "/test-file.go", "foo")
    FileUtils.writeToFile(testDirectory.toString + "/setup.go", "bar")
  }

  private def setupFileSystemEmpty(): Unit =
    testDirectory.toFile.mkdirs

  it should "filter correctly when setup.go exists" in {
    setupFileSystemWhole()
    val output = RegenerationFilter.filter(
      testDirectory.toString,
      RegenerationFilterTestData.simpleTestProjectWithConflict,
      PoliceSergeantNicholasAngel,
    )
    output match {
      case Some(project) =>
        projectFilesShouldMatch(
          project,
          Map(
            FileSystem.File("", "test-file.go") -> "test-contents",
          ),
        )
      case None => fail
    }
  }

  it should "not filter when no files exist" in {
    setupFileSystemEmpty()
    val output = RegenerationFilter.filter(
      testDirectory.toString,
      RegenerationFilterTestData.simpleTestProjectWithConflict,
      PoliceSergeantNicholasAngel,
    )
    output shouldBe Some(RegenerationFilterTestData.simpleTestProjectWithConflict)
    output match {
      case Some(project) =>
        projectFilesShouldMatch(project, RegenerationFilterTestData.simpleTestProjectWithConflict.files)
      case None => fail()
    }
  }

  it should "do nothing when the user says no" in {
    setupFileSystemWhole()
    val output = RegenerationFilter.filter(
      testDirectory.toString,
      RegenerationFilterTestData.simpleTestProjectWithConflict,
      questionAsker = _ => "n", // Reply no when asked to overwrite
    )
    output shouldBe None
  }

}
