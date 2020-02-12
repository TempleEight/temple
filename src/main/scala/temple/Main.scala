package temple

import temple.DSL.DSLProcessor
import temple.utils.FileUtils

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
    val outputDirectory = config.Generate.outputDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents    = FileUtils.readFile(config.Generate.filename())
    val result          = DSLProcessor.parse(fileContents)
    println(s"Generated... $result - will place in $outputDirectory")
  }
}
