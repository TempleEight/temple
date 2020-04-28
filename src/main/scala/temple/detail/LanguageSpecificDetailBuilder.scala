package temple.detail

import temple.ast.Metadata.ServiceLanguage
import temple.ast.{Metadata, Templefile}
import temple.builder.project.ProjectConfig
import temple.detail.LanguageDetail.GoLanguageDetail

/** Call out to the user and ask for any required extra input based on their Templefile options */
object LanguageSpecificDetailBuilder {
  class InvalidPackageNameException(str: String) extends RuntimeException(str)

  private def buildGoDetail(questionAsker: QuestionAsker): GoLanguageDetail = {
    var packageName = ""
    while (packageName == "") {
      packageName = questionAsker.askQuestion(
        "What should the Go module name be? (expected format \"github.com/username/repo\")",
      )
    }

    if (packageName.contains(" ")) {
      throw new InvalidPackageNameException("Please enter a valid Go module name, it cannot contain whitespace")
    }
    GoLanguageDetail(packageName)
  }

  /** Given a [[Templefile]] and a [[QuestionAsker]], build the additional details required to generate */
  def build(templefile: Templefile, questionAsker: QuestionAsker): LanguageDetail = {
    val language =
      templefile.projectBlock.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    language match {
      case ServiceLanguage.Go => buildGoDetail(questionAsker)
    }
  }
}
