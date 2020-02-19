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
    def shouldNotParse: String  = parsed.swap.fromEither(res => fail(s"Unexpected successful parse to $res"))
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

  it should "not allow parameters for foreign keys" in {
    DSLProcessor.parse("Test: service {age: int; Person: struct { test: Test }}").shouldParse
    DSLProcessor.parse("Test: service {age: int; Person: struct { test: Test() }}").shouldNotParse
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
          Attribute("username", AttributeType.Primitive("string")),
          Attribute("email", AttributeType.Primitive("string", Args(Seq(Arg.IntArg(40), Arg.IntArg(5))))),
          Attribute("firstName", AttributeType.Primitive("string")),
          Attribute("lastName", AttributeType.Primitive("string")),
          Attribute("createdAt", AttributeType.Primitive("datetime")),
          Attribute("numberOfDogs", AttributeType.Primitive("int")),
          Attribute("yeets", AttributeType.Primitive("bool"), Seq(Annotation("unique"), Annotation("server"))),
          Attribute(
            "currentBankBalance",
            AttributeType
              .Primitive("float", Args(kwargs = Seq("min" -> Arg.FloatingArg(0), "precision" -> Arg.IntArg(2)))),
          ),
          Attribute("birthDate", AttributeType.Primitive("date")),
          Attribute("breakfastTime", AttributeType.Primitive("time")),
          DSLRootItem(
            "Fred",
            "struct",
            Seq(
              Attribute("field", AttributeType.Primitive("string"), Seq(Annotation("nullable"))),
              Attribute("friend", AttributeType.Foreign("User")),
              Metadata("enumerable", Args(kwargs = Seq("by" -> Arg.TokenArg("friend")))),
            ),
          ),
          Metadata("enumerable"),
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
