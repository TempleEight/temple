package temple.detail

import temple.ast.Metadata.ServiceLanguage
import temple.ast.{Metadata, Templefile}
import temple.builder.project.ProjectConfig
import temple.detail.Detail.GoDetail

/** Call out to the user and ask for any required extra input based on their templefile options */
object LanguageSpecificDetailBuilder {

  private def buildGoDetail(templefile: Templefile, questionAsker: QuestionAsker): Detail = {
    val packageName = questionAsker.askQuestion(
      "Yo pal, what is the Go module name you'd like (expected format \"github.com/yeeter69/lockdown\")?",
    )
    GoDetail(packageName)
  }

  /** Given a templefile and a question asker, build all the required details */
  def build(templefile: Templefile, questionAsker: QuestionAsker): Detail = {
    val language =
      templefile.projectBlock.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    language match {
      case ServiceLanguage.Go => buildGoDetail(templefile, questionAsker)
    }
  }
}
