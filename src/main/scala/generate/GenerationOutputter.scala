package generate

import java.io.{File, PrintWriter}

import generate.database.DatabaseGenerator

class GenerationOutputter(filename: String) {
  val writer = new PrintWriter(new File(filename))

  def output(generator: DatabaseGenerator): Unit = {
    this.writer.write(generator.getText)
  }

  def close(): Unit = {
    this.writer.close()
  }

}
