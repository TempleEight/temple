package temple.detail

import temple.ast.Metadata.ServiceLanguage
import temple.ast.{Metadata, Templefile}
import temple.builder.project.ProjectConfig
import temple.detail.LanguageDetail.GoLanguageDetail

/** Call out to the user and ask for any required extra input based on their Templefile options */
object LanguageSpecificDetailBuilder {

  private def buildGoDetail(templefile: Templefile, questionAsker: QuestionAsker): GoLanguageDetail = {
    val packageName = questionAsker.askQuestion(
      "What should the Go module name be? (expected format \"github.com/username/repo\")",
    )
    if (packageName.length == 0) {
      throw new RuntimeException("Please enter a Go module name.")
    }
    if (packageName.contains(" ")) {
      throw new RuntimeException("Please enter a valid Go module name.")
    }
    GoLanguageDetail(packageName)
  }

  /** Given a [[Templefile]] and a [[QuestionAsker]], build the additional details required to generate */
  def build(templefile: Templefile, questionAsker: QuestionAsker): LanguageDetail = {
    val language =
      templefile.projectBlock.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    language match {
      case ServiceLanguage.Go => buildGoDetail(templefile, questionAsker)
    }
  }
}
