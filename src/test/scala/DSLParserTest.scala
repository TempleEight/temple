import org.scalatest.{FlatSpec, Matchers}
import temple.DSL._
import TestUtils._
import temple.DSL

class DSLParserTest extends FlatSpec with Matchers {
  "Empty string" should "parse" in {
    DSLParser.parse("").isRight shouldBe true
  }

  "Empty service" should "parse" in {
    DSLParser.parse("Test: service { }").isRight shouldBe true
  }

  "Annotations" should "not parse at the top level" in {
    DSLParser.parse("@server Test: service { }").isLeft shouldBe true
  }

  "Simple.temple" should "parse" in {
    val source      = readFile("src/test/scala/testfiles/simple.temple")
    val parseResult = DSLParser.parse(source).fromEither(msg => fail(s"first parse failed, $msg"))
    val reSourced   = parseResult.mkString("\n\n")

    val reParsedResult = DSLParser.parse(reSourced).fromEither(msg => fail(s"second parse failed, $msg"))
    parseResult shouldEqual reParsedResult
  }
}
