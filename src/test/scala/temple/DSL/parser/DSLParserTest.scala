package temple.DSL.parser

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.DSLProcessor
import temple.DSL.syntax.Entry.{Attribute, Metadata}
import temple.DSL.syntax._
import temple.utils.FileUtils._
import temple.utils.MonadUtils.FromEither

class DSLParserTest extends FlatSpec with Matchers {
  behavior of "DSLParser"

  it should "parse an empty string" in {
    DSLProcessor.parse("").isRight shouldBe true
  }

  it should "parse an empty service" in {
    DSLProcessor.parse("Test: service { }").isRight shouldBe true
  }

  it should "not parse annotation at the top level" in {
    DSLProcessor.parse("@server Test: service { }").isLeft shouldBe true
  }

  it should "parse to the correct result for simple.temple" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).fromEither(msg => fail(s"simple.temple did not parse, $msg"))

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
            Seq(Attribute("field", AttributeType.Primitive("string"), Seq(Annotation("nullable")))),
          ),
          Metadata("auth", Args(kwargs = Seq("login" -> Arg.TokenArg("username")))),
          Metadata("uses", Args(Seq(Arg.ListArg(Seq(Arg.TokenArg("Booking"), Arg.TokenArg("Events")))))),
        ),
      ),
    )
  }

  it should "re-parse to the same result if a parsed structure is exported to string" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).fromEither(msg => fail(s"first parse failed, $msg"))
    val reSourced   = parseResult.mkString("\n\n")

    val reParsedResult = DSLProcessor.parse(reSourced).fromEither(msg => fail(s"second parse failed, $msg"))
    parseResult shouldBe reParsedResult
  }
}
