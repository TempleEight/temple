package temple

import java.nio.file.FileAlreadyExistsException

import temple.detail.StdInQuestionAsker

/** Main entry point into the application */
object Main extends App {

  def exit(msg: String): Nothing = {
    System.err.println(msg)
    sys.exit(1)
  }

  try {
    val config = new TempleConfig(args)
    config.subcommand match {
      case Some(config.Generate) => Application.generate(config, StdInQuestionAsker)
      case Some(config.Validate) => Application.validate(config)
      case Some(config.Test)     => Application.test(config)
      case Some(_)               => throw new TempleConfig.UnhandledArgumentException
      case None                  => config.printHelp()
    }
  } catch {
    case error: FileAlreadyExistsException => exit(s"File already exists: ${error.getMessage}")
    case error: Exception                  => exit(error.getMessage)
  }

}
