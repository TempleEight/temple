package temple.detail

import scala.io.StdIn

/** Given a question, reaches out to StdIn to ask the user for input */
object StdInQuestionAsker extends QuestionAsker {

  /** Given a question string, get the user to respond, returning their answer */
  override def askQuestion(question: String): String =
    StdIn.readLine(Console.BLUE + question + "\n" + Console.RESET)
}
