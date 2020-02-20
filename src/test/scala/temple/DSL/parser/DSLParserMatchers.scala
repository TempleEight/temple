package temple.DSL.parser

import org.scalatest.Assertions
import temple.DSL.syntax.Templefile
import temple.utils.MonadUtils.FromEither

trait DSLParserMatchers extends Assertions {

  implicit protected class ParseResult(parsed: Either[String, Templefile]) {
    def shouldParse: Templefile = parsed.fromEither(msg => fail(s"Parse error: $msg"))
    def shouldNotParse: String  = parsed.swap.fromEither(res => fail(s"Unexpected successful parse to $res"))
  }
}
