package temple.detail

object MockQuestionAsker extends QuestionAsker {

  /** Actually ask the relevant question */
  override def askQuestion(question: String): String = "github.com/squat/and/dab"
}
