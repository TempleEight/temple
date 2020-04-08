package temple.utils

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import temple.builder.project.Project

import scala.io.{Source, StdIn}

/** Helper functions useful for manipulating files */
object FileUtils {

  def outputProject(directory: String, project: Project): Unit = {
    FileUtils.createDirectory(directory)
    project.files.foreach {
      case (file, contents) =>
        val subfolder = s"$directory/${file.folder}"
        FileUtils.createDirectory(subfolder)
        FileUtils.writeToFile(
          s"$subfolder/${file.filename}",
          contents,
        )
    }
  }

  def createDirectory(directory: String): Unit =
    Files.createDirectories(Paths.get(directory))

  /** Write a string to file */
  def writeToFile(filename: String, s: String): Unit = {
    val path   = Paths.get(filename)
    val writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))
    try writer.write(s)
    finally writer.close()
  }

  /** Read a string from a file */
  def readFile(filename: String): String = {
    val path = Paths.get(filename)
    Files.readString(path)
  }

  /** Read a string from standard input */
  def readStdIn(): String = Iterator.continually(StdIn.readLine).takeWhile(_ != null).mkString("\n")

  /** Read a string from a file in the resources folder */
  def readResources(filename: String): String =
    Source.fromResource(filename).mkString

  /** Read a byte array from a file */
  def readBinaryFile(filename: String): Array[Byte] = {
    val path = Paths.get(filename)
    Files.readAllBytes(path)
  }
}
