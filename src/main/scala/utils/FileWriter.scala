package utils

import java.io.{File, PrintWriter}

object utils {

  def writeToFile(filename: String, s: String): Unit = {
    val writer = new PrintWriter(new File(filename))
    writer.write(s)
    writer.close
  }

}
