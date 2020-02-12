package temple.utils

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

import scala.io.Source

/** Helper functions useful for manipulating files */
object FileUtils {

  def createDirectory(directory: String): Unit =
    Files.createDirectories(Paths.get(directory))

  /** Write a string to file */
  def writeToFile(filename: String, s: String): Unit = {
    val writer = new PrintWriter(new File(filename))
    try writer.write(s)
    finally writer.close()
  }

  /** Read a string from a file */
  def readFile(filename: String): String = {
    val file = Source.fromFile(name = filename)
    try file.mkString
    finally file.close()
  }
}
