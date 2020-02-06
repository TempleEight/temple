package temple.DSL

import temple.DSL.Syntax.DSLRootItem
import temple.DSL.parser.DSLParser

object DSLProcessor extends DSLParser {

  /** Show an error message for a failed parse, with the line and number printed */
  private def printError(input: Input): String = {
    val lineNo = input.pos.line.toString
    lineNo + " | " + input.source.toString.split("\n")(input.pos.line - 1) + "\n" +
    (" " * (input.pos.column + 2 + lineNo.length)) + "^"
  }

  /**
    * Parse the contents of a Temple file
    * @param contents the contents of a Templefile as a string
    * @return Right of the the parsed list of root elements, or Left of a string representing the error
    */
  def parse(contents: String): Either[String, Syntax.Templefile] =
    parseAll(templefile, contents) match {
      case Success(result, _)    => Right(result)
      case NoSuccess(str, input) => Left(str + '\n' + printError(input))
    }
}
