import org.scalatest.FunSuite
import temple.DSL.DSLParser
import scala.io.Source
import temple.DSL

class DSLParserTest extends FunSuite {
  test("DSLParser.emptyString") {
    assert(DSLParser.parse("").isLeft)
  }

  test("DSLParser.emptyService") {
    assert(DSLParser.parse("Test: service { }").isRight)
  }

  test("DSLParser.topLevelAnnotation") {
    assert(DSLParser.parse("@server Test: service { }").isLeft)
  }

  test("DSLParser.validSimpleInput") {
    val fileSource  = Source.fromFile("src/test/scala/testfiles/simple.temple").mkString
    val parseResult = DSLParser.parse(fileSource)
    assert(parseResult.isRight)

    val removeWhitespace = fileSource.split("\n").filter(_.nonEmpty).mkString("\n")
    assert(removeWhitespace == parseResult.right.get.toString())
  }
}
