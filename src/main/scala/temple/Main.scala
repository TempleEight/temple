package temple

import temple.DSL.DSLProcessor
import temple.utils.FileUtils

/** Main entry point into the application */
object Main extends App {
  try {
    val config = new TempleConfig(args)
    config.subcommand match {
      case Some(config.generate) => generate(config)
      case Some(_)               => throw new TempleConfig.UnhandledArgumentException
      case None                  => config.printHelp()
    }
  } catch {
    case error: IllegalArgumentException =>
      System.err.println(error.getMessage)
      sys.exit(1)
  }

  def generate(config: TempleConfig): Unit = {
    val fileContents = FileUtils.readFile(config.generate.filename())
    val result       = DSLProcessor.parse(fileContents)
    println(s"Generated... ${result}")
  }
}
