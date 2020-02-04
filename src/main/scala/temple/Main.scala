package temple

import temple.DSL.DSLProcessor
import temple.utils.FileUtils

// $COVERAGE-OFF$

/** Main entry point into the application */
object Main extends App {
  try {
    val config = new TempleConfig(args)
    config.subcommand match {
      case Some(config.Generate) => generate(config)
      case Some(_)               => throw new TempleConfig.UnhandledArgumentException
      case None                  => config.printHelp()
    }
  } catch {
    case error: IllegalArgumentException =>
      System.err.println(error.getMessage)
      sys.exit(1)
  }

  def generate(config: TempleConfig): Unit = {
    val fileContents = FileUtils.readFile(config.Generate.filename())
    val result       = DSLProcessor.parse(fileContents)
    println(s"Generated... $result")
  }
}
