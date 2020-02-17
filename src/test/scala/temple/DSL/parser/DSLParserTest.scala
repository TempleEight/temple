package temple.DSL.parser

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.DSLProcessor
import temple.DSL.syntax.Entry.{Attribute, Metadata}
import temple.DSL.syntax._
import temple.utils.FileUtils._
import temple.utils.MonadUtils.FromEither

class DSLParserTest extends FlatSpec with Matchers {

  implicit private class ParseResult(parsed: Either[String, Templefile]) {
    def shouldParse: Templefile = parsed.fromEither(msg => fail(s"Parse error: $msg"))
    def shouldNotParse: Unit    = parsed.isLeft shouldBe true
  }

  behavior of "DSLParser"

  it should "parse an empty string" in {
    DSLProcessor.parse("").shouldParse
  }

  it should "parse an empty service" in {
    DSLProcessor.parse("Test: service { }").shouldParse
  }

  it should "not parse annotation at the top level" in {
    DSLProcessor.parse("@server Test: service { }").shouldNotParse
  }

  it should "parse to the correct result for simple.temple" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).shouldParse

    parseResult shouldBe Seq(
      DSLRootItem("SimpleTempleTest", "project", Nil),
      DSLRootItem(
        "User",
        "service",
        Seq(
          Attribute("username", AttributeType("string")),
          Attribute("email", AttributeType("string", Args(Seq(Arg.IntArg(40), Arg.IntArg(5))))),
          Attribute("firstName", AttributeType("string")),
          Attribute("lastName", AttributeType("string")),
          Attribute("createdAt", AttributeType("datetime")),
          Attribute("numberOfDogs", AttributeType("int")),
          Attribute("yeets", AttributeType("bool"), Seq(Annotation("unique"), Annotation("server"))),
          Attribute(
            "currentBankBalance",
            AttributeType("float", Args(kwargs = Seq("min" -> Arg.FloatingArg(0), "precision" -> Arg.IntArg(2)))),
          ),
          Attribute("birthDate", AttributeType("date")),
          Attribute("breakfastTime", AttributeType("time")),
          DSLRootItem("Fred", "struct", Seq(Attribute("field", AttributeType("string"), Seq(Annotation("nullable"))))),
          Metadata("auth", Args(kwargs = Seq("login" -> Arg.TokenArg("username")))),
          Metadata("uses", Args(Seq(Arg.ListArg(Seq(Arg.TokenArg("Booking"), Arg.TokenArg("Events")))))),
        ),
      ),
    )
  }

  it should "re-parse to the same result if a parsed structure is exported to string" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).shouldParse
    val reSourced   = parseResult.mkString("\n\n")

    val reParsedResult = DSLProcessor.parse(reSourced).fromEither(msg => fail(s"second parse failed, $msg"))
    parseResult shouldBe reParsedResult
  }
}
