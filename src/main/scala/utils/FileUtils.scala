package utils

import java.io.{File, PrintWriter}

/** Helper functions useful for manipulating files */
object FileUtils {

  /** Write a string to file */
  def writeToFile(filename: String, s: String): Unit = {
    val writer = new PrintWriter(new File(filename))
    try writer.write(s)
    finally writer.close()
  }
}
