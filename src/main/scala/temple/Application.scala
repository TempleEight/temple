package temple

import temple.DSL.DSLProcessor
import temple.DSL.semantics.{Analyzer, SemanticParsingException}
import temple.builder.project.ProjectBuilder
import temple.detail.{LanguageSpecificDetailBuilder, QuestionAsker}
import temple.test.ProjectTester
import temple.utils.FileUtils
import temple.utils.MonadUtils.FromEither

object Application {
  class InvalidTemplefileException(str: String) extends RuntimeException(str)

  private def readFileOrStdIn(filename: String): String =
    if (filename == "-") FileUtils.readStdIn() else FileUtils.readFile(filename)

  def generate(config: TempleConfig, questionAsker: QuestionAsker): Unit = {
    val outputDirectory = config.Generate.outputDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents    = readFileOrStdIn(config.Generate.filename())
    val data = DSLProcessor.parse(fileContents) fromEither { error =>
      throw new InvalidTemplefileException(error)
    }
    val analyzedTemplefile = Analyzer.parseAndValidate(data)
    val detail             = LanguageSpecificDetailBuilder.build(analyzedTemplefile, questionAsker)
    val project            = ProjectBuilder.build(analyzedTemplefile, detail)
    val filteredProject = RegenerationFilter.filter(outputDirectory, project, questionAsker).getOrElse {
      println("Nothing to generate")
      return
    }
    FileUtils.outputProject(outputDirectory, filteredProject)
    println(s"Generated project in $outputDirectory")
  }

  def validate(config: TempleConfig): Unit = {
    val fileContents = readFileOrStdIn(config.Validate.filename())
    val data = DSLProcessor.parse(fileContents) fromEither { error =>
      throw new InvalidTemplefileException("Templefile could not be parsed\n" + error)
    }
    try {
      Analyzer.parseAndValidate(data)
      println(s"Templefile validated correctly")
    } catch {
      case error: SemanticParsingException =>
        throw new InvalidTemplefileException("Templefile could not be validated\n" + error.getMessage)
    }
  }

  def test(config: TempleConfig): Unit = {
    val generatedDirectory = config.Test.generatedDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents       = readFileOrStdIn(config.Test.filename())
    val data = DSLProcessor.parse(fileContents) fromEither { error =>
      throw new InvalidTemplefileException(error)
    }
    val analyzedTemplefile = Analyzer.parseAndValidate(data)
    if (config.Test.testOnly()) {
      ProjectTester.testOnly(analyzedTemplefile, generatedDirectory)
    } else {
      ProjectTester.test(analyzedTemplefile, generatedDirectory)
    }
  }
}
