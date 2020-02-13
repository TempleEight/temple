package temple.utils

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

/** Helper functions useful for manipulating files */
object FileUtils {

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
}
