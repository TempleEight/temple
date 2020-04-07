package temple.DSL.parser

import org.scalactic.source.Position
import org.scalatest.{Assertion, Matchers}
import temple.DSL.syntax.Templefile
import temple.utils.MonadUtils.FromEither

trait DSLParserMatchers extends Matchers {

  protected object parse
  protected object parseError

  protected trait ShouldParseError {
    def withMessage(message: String): Assertion
  }

  implicit protected class ParseResult(parsed: Either[String, Templefile]) {

    def should(parseWord: parse.type)(implicit here: Position): Templefile =
      parsed.fromEither(msg => fail(s"Parse error: $msg"))

    def should(parseErrorWord: parseError.type)(implicit here: Position): ShouldParseError =
      message => parsed.swap.fromEither(res => fail(s"Unexpected successful parse to $res")) shouldBe message

  }
}
