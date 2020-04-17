package temple

import temple.DSL.DSLProcessor
import temple.DSL.semantics.{Analyzer, SemanticParsingException}
import temple.builder.project.ProjectBuilder
import temple.detail.{LanguageSpecificDetailBuilder, QuestionAsker}
import temple.test.ProjectTester
import temple.utils.FileUtils
import temple.utils.MonadUtils.FromEither

object Application {

  private def readFileOrStdIn(filename: String): String =
    if (filename == "-") FileUtils.readStdIn() else FileUtils.readFile(filename)

  def generate(config: TempleConfig, questionAsker: QuestionAsker): Unit = {
    val outputDirectory = config.Generate.outputDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents    = readFileOrStdIn(config.Generate.filename())
    val data = DSLProcessor.parse(fileContents) fromEither { error =>
      throw new RuntimeException(error)
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
      throw new RuntimeException("Templefile could not be parsed\n" + error)
    }
    try {
      Analyzer.parseAndValidate(data)
      println(s"Templefile validated correctly")
    } catch {
      case error: SemanticParsingException =>
        throw new RuntimeException("Templefile could not be validated\n" + error.getMessage, error)
    }
  }

  def test(config: TempleConfig): Unit = {
    val generatedDirectory = config.Test.generatedDirectory.getOrElse(System.getProperty("user.dir"))
    val fileContents       = readFileOrStdIn(config.Test.filename())
    val data = DSLProcessor.parse(fileContents) fromEither { error =>
      throw new RuntimeException(error)
    }
    val analyzedTemplefile = Analyzer.parseAndValidate(data)
    ProjectTester.test(analyzedTemplefile, generatedDirectory)
  }
}
