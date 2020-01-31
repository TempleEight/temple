package temple

import temple.DSL.DSLProcessor
import temple.utils.FileUtils

object Main extends App {
  val config = new TempleConfig(args)
  config.subcommand match {
    case Some(config.generate) => generate(config)
    case _                     => config.printHelp()
  }

  def generate(config: TempleConfig): Unit = {
    val fileContents = FileUtils.readFile(config.generate.filename())
    val result       = DSLProcessor.parse(fileContents)
    println(s"Generated... ${result}")
  }
}
