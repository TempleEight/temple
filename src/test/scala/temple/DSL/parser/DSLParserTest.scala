package temple.DSL.parser

import org.scalatest.FlatSpec
import temple.DSL.DSLProcessor
import temple.DSL.syntax.Entry.{Attribute, Metadata}
import temple.DSL.syntax._
import temple.utils.FileUtils._
import temple.utils.MonadUtils.FromEither

class DSLParserTest extends FlatSpec with DSLParserMatchers {

  behavior of "DSLParser"

  it should "parse an empty string" in {
    DSLProcessor.parse("") should parse
  }

  it should "parse an empty service" in {
    DSLProcessor.parse("Test: service { }") should parse
  }

  it should "not parse with underscores" in {
    DSLProcessor.parse("Test_project: service { }") should parseError withMessage {
      """':' expected but '_' found
        |1 | Test_project: service { }
        |        ^
        |""".stripMargin.trim
    }
  }

  it should "not parse annotation at the top level" in {
    DSLProcessor.parse("@server Test: service { }") should parseError withMessage {
      """string matching regex '[A-Z]' expected but '@' found
        |1 | @server Test: service { }
        |    ^
        |""".stripMargin.trim
    }
  }

  it should "not allow parameters for foreign keys" in {
    DSLProcessor.parse("Test: service {age: int; Person: struct { test: Test }}") should parse
    DSLProcessor.parse("Test: service {age: int; Person: struct { test: Test() }}") should parseError withMessage {
      """Foreign keys cannot have parameters
        |1 | Test: service {age: int; Person: struct { test: Test() }}
        |                                                        ^
        |""".stripMargin.trim
    }
  }

  it should "parse to the correct result for simple.temple" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source) should parse

    parseResult shouldBe Seq(
      DSLRootItem("SimpleTempleTest", "project", Nil),
      DSLRootItem(
        "User",
        "service",
        Seq(
          Attribute("user", AttributeType.Primitive("string")),
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
              Attribute("image", AttributeType.Primitive("data", Args(Seq(Arg.IntArg(10_000_000))))),
              Metadata("enumerable"),
              Metadata("readable", Args(kwargs = Seq("by" -> Arg.TokenArg("this")))),
            ),
          ),
          Metadata("enumerable"),
          Metadata("omit", Args(Seq(Arg.ListArg(Seq(Arg.TokenArg("delete")))))),
          Metadata("readable", Args(kwargs = Seq("by" -> Arg.TokenArg("all")))),
          Metadata("writable", Args(kwargs = Seq("by" -> Arg.TokenArg("this")))),
          Metadata("auth", Args(Seq(Arg.TokenArg("email")))),
          Metadata("uses", Args(Seq(Arg.ListArg(Seq(Arg.TokenArg("Booking"), Arg.TokenArg("Group")))))),
        ),
      ),
      DSLRootItem("Booking", "service", Seq()),
      DSLRootItem("Group", "service", Seq()),
    )
  }

  it should "re-parse to the same result if a parsed structure is exported to string" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source) should parse
    val reSourced   = parseResult.mkString("\n\n")

    val reParsedResult = DSLProcessor.parse(reSourced).fromEither(msg => fail(s"second parse failed, $msg"))
    parseResult shouldBe reParsedResult
  }
}
