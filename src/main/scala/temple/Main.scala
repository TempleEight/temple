package temple

import java.nio.file.FileAlreadyExistsException

import temple.DSL.DSLProcessor
import temple.DSL.semantics.{Analyser, SemanticParsingException}
import temple.builder.project.ProjectBuilder
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
    case error: SemanticParsingException   => exit(error.getMessage)
  }

  def generate(config: TempleConfig): Unit = {
    val outputDirectory = config.Generate.outputDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents    = FileUtils.readFile(config.Generate.filename())
    DSLProcessor.parse(fileContents) match {
      case Left(error) => exit(error)
      case Right(data) =>
        val analysedTemplefile = Analyser.parseSemantics(data)
        val project            = ProjectBuilder.build(analysedTemplefile)

        FileUtils.createDirectory(outputDirectory)
        project.databaseCreationScripts.foreach {
          case (file, contents) =>
            val subfolder = s"$outputDirectory/${file.folder}"
            FileUtils.createDirectory(subfolder)
            FileUtils.writeToFile(
              s"$subfolder/${file.filename}.${file.filetype.toString}",
              contents,
            )
        }
        println(s"Generated project in $outputDirectory")
    }
  }
}
