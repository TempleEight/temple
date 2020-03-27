package temple

import temple.DSL.DSLProcessor
import temple.DSL.semantics.Analyzer
import temple.builder.project.ProjectBuilder
import temple.utils.FileUtils
import temple.utils.MonadUtils.FromEither

object Application {

  def generate(config: TempleConfig): Unit = {
    val outputDirectory = config.Generate.outputDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents    = FileUtils.readFile(config.Generate.filename())
    val data = DSLProcessor.parse(fileContents) fromEither { error =>
      throw new RuntimeException(error)
    }
    val analyzedTemplefile = Analyzer.parseAndValidate(data)
    val project            = ProjectBuilder.build(analyzedTemplefile)

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
