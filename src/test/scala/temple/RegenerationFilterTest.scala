package temple

import java.io.{File, PrintWriter}

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import temple.builder.project.Project
import temple.detail.PoliceSergeantNicholasAngel
import temple.generate.FileSystem

import scala.reflect.io.Directory

class RegenerationFilterTest extends FlatSpec with Matchers with BeforeAndAfter {

  behavior of "RegenerationFilterTest"

  private val testDirectory = "/tmp/test"

  after {
    val testDirectory = new File(this.testDirectory)
    if (testDirectory.exists()) new Directory(testDirectory).deleteRecursively()
  }

  private def setupFileSystemWhole(): Unit = {
    new File(testDirectory).mkdirs
    val testFile = new File(testDirectory + "/test-file.go")
    new PrintWriter(testFile).write("foo")
    val setupGo = new File(testDirectory + "/setup.go")
    new PrintWriter(setupGo).write("bar")
  }

  private def setupFileSystemEmpty(): Unit =
    new File(testDirectory).mkdirs

  it should "filter correctly when setup.go exists" in {
    setupFileSystemWhole()
    val output = RegenerationFilter.filter(
      testDirectory,
      RegenerationFilterTestData.simpleTestProjectWithConflict,
      PoliceSergeantNicholasAngel,
    )
    output shouldBe Project(
      Map(
        FileSystem.File("", "test-file.go") -> "test-contents",
      ),
    )
  }

  it should "not filter when no files exist" in {
    setupFileSystemEmpty()
    val output = RegenerationFilter.filter(
      testDirectory,
      RegenerationFilterTestData.simpleTestProjectWithConflict,
      PoliceSergeantNicholasAngel,
    )
    output shouldBe RegenerationFilterTestData.simpleTestProjectWithConflict
  }

  it should "do nothing when the user says no" in {
    setupFileSystemWhole()
    val output = RegenerationFilter.filter(
      testDirectory,
      RegenerationFilterTestData.simpleTestProjectWithConflict,
      (_: String) => "n", // SAM implementation of a QuestionAsker that always says no
    )
    output shouldBe Project(Map())
  }

}
