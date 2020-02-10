package temple.DSL.parser

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.Entry.{Attribute, Metadata}
import temple.DSL.{Arg, AttributeType, DSLProcessor, DSLRootItem}
import temple.utils.FileUtils._
import temple.utils.MonadUtils.FromEither

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

  "simple.temple" should "parse to the correct result" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).fromEither(msg => fail(s"simple.temple did not parse, $msg"))

    parseResult shouldBe Seq(
      DSLRootItem(
        "User",
        "service",
        Seq(
          Attribute("username", AttributeType("string")),
          Attribute("email", AttributeType("string", Seq(Arg.IntArg(40), Arg.IntArg(5)))),
          Attribute("firstName", AttributeType("string")),
          Attribute("lastName", AttributeType("string")),
          Attribute("createdAt", AttributeType("datetimetz")),
          Attribute("numberOfDogs", AttributeType("int")),
          Attribute("yeets", AttributeType("bool")),
          Attribute(
            "currentBankBalance",
            AttributeType("float", kwargs = Seq("min" -> Arg.FloatingArg(0), "precision" -> Arg.IntArg(2)))
          ),
          Attribute("birthDate", AttributeType("date")),
          Attribute("breakfastTime", AttributeType("time")),
          DSLRootItem("Fred", "struct", Seq(Attribute("field", AttributeType("string")))),
          Metadata("auth", kwargs = Seq("login" -> Arg.TokenArg("username"))),
          Metadata("uses", Seq(Arg.ListArg(Seq(Arg.TokenArg("Booking"), Arg.TokenArg("Events")))))
        )
      )
    )
  }

  "Exporting a parsed structure to string" should "re-parse to the same result" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).fromEither(msg => fail(s"first parse failed, $msg"))
    val reSourced   = parseResult.mkString("\n\n")

    val reParsedResult = DSLProcessor.parse(reSourced).fromEither(msg => fail(s"second parse failed, $msg"))
    parseResult shouldBe reParsedResult
  }

  // TODO: add: two things parse to the same result
}
