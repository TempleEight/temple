package temple.detail

/** Used to ask questions and get a response, handles all I/O so it can nicely be mocked for tests */
trait QuestionAsker {

  /** Actually ask the relevant question */
  def askQuestion(question: String): String
}
