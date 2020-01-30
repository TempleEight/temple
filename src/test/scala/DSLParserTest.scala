import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.DSLProcessor
import utils.FileUtils._
import utils.MonadUtils.FromEither

class DSLParserTest extends FlatSpec with Matchers {
  "Empty string" should "parse" in {
    DSLProcessor.parse("").isRight shouldBe true
  }

  "Empty service" should "parse" in {
    DSLProcessor.parse("Test: service { }").isRight shouldBe true
  }

  "Annotations" should "not parse at the top level" in {
    DSLProcessor.parse("@server Test: service { }").isLeft shouldBe true
  }

  "Simple.temple" should "parse" in {
    val source      = readFile("src/test/scala/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).fromEither(msg => fail(s"first parse failed, $msg"))
    val reSourced   = parseResult.mkString("\n\n")

    val reParsedResult = DSLProcessor.parse(reSourced).fromEither(msg => fail(s"second parse failed, $msg"))
    parseResult shouldEqual reParsedResult
  }
}
