package temple

import java.nio.file.FileAlreadyExistsException

import temple.DSL.DSLProcessor
import temple.utils.FileUtils

/** Main entry point into the application */
object Main extends App {

  def exit(msg: String): Nothing = {
    System.err.println(msg)
    sys.exit(1)
  }

  try {
    val config = new TempleConfig(args)
    config.subcommand match {
      case Some(config.Generate) => generate(config)
      case Some(_)               => throw new TempleConfig.UnhandledArgumentException
      case None                  => config.printHelp()
    }
  } catch {
    case error: IllegalArgumentException   => exit(error.getMessage)
    case error: FileAlreadyExistsException => exit(s"File already exists: ${error.getMessage}")
  }

  def generate(config: TempleConfig): Unit = {
    val outputDirectory = config.Generate.outputDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents    = FileUtils.readFile(config.Generate.filename())
    DSLProcessor.parse(fileContents) match {
      case Left(error) => exit(error)
      case Right(data) =>
        FileUtils.createDirectory(outputDirectory)
        FileUtils.writeToFile(outputDirectory + "/test.out", data.toString())
        println(s"Generated file in $outputDirectory/test.out")
    }
  }
}
