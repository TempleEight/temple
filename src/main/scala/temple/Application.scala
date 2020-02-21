package temple

import temple.DSL.DSLProcessor
import temple.DSL.semantics.Analyser
import temple.builder.project.ProjectBuilder
import temple.utils.FileUtils

object Application {

  def generate(config: TempleConfig): Unit = {
    val outputDirectory = config.Generate.outputDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents    = FileUtils.readFile(config.Generate.filename())
    DSLProcessor.parse(fileContents) match {
      case Left(error) => throw new RuntimeException(error)
      case Right(data) =>
        val analysedTemplefile = Analyser.parseSemantics(data)
        val project            = ProjectBuilder.build(analysedTemplefile)

        FileUtils.createDirectory(outputDirectory)
        project.files.foreach {
          case (file, contents) =>
            val subfolder = s"$outputDirectory/${file.folder}"
            FileUtils.createDirectory(subfolder)
            FileUtils.writeToFile(
              s"$subfolder/${file.filename}",
              contents,
            )
        }
        println(s"Generated project in $outputDirectory")
    }
  }
}
