package temple.DSL.parser

import org.scalactic.source.Position
import org.scalatest.Assertions
import temple.DSL.syntax.Templefile
import temple.utils.MonadUtils.FromEither

trait DSLParserMatchers extends Assertions {

  implicit protected class ParseResult(parsed: Either[String, Templefile]) {
    def shouldParse(implicit here: Position): Templefile = parsed.fromEither(msg => fail(s"Parse error: $msg"))

    def shouldNotParse(implicit here: Position): String =
      parsed.swap.fromEither(res => fail(s"Unexpected successful parse to $res"))
  }
}
