package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.Syntax
import temple.DSL.Syntax.{Args, DSLRootItem, Entry}
import temple.DSL.semantics.Analyser.parseSemantics

class SemanticAnalyserTest extends FlatSpec with Matchers {
  "Semantic Analyser" should "complain that there is no project block when parsing an Empty AST" in {
    a[SemanticParsingException] should be thrownBy { parseSemantics(Nil) }
  }

  private def mkTemplefileAST(rootItems: DSLRootItem*): Syntax.Templefile =
    DSLRootItem("test", "project", Nil) +: rootItems

  private def mkTemplefileSemantics(entries: (String, ServiceBlock)*): Templefile =
    Templefile("test", Nil, Map.empty, entries.toMap)

  private def mkTemplefileASTWithUserService(entries: Entry*): Syntax.Templefile = Seq(
    DSLRootItem("test", "project", Nil),
    DSLRootItem("User", "service", entries)
  )

  private def mkTemplefileSemanticsWithUserService(serviceBlock: ServiceBlock): Templefile =
    Templefile("test", Nil, Map.empty, Map("User" -> serviceBlock))

  "Semantic Analyser" should "parse an AST containing only an empty project block" in {
    parseSemantics(mkTemplefileAST()) shouldBe mkTemplefileSemantics()
  }

  "Semantic Analyser" should "parse an AST containing a basic user service" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        Entry.Attribute("index", Syntax.AttributeType("int", Args()))
      )
    ) shouldEqual mkTemplefileSemanticsWithUserService(
      ServiceBlock(Map("index" -> Attribute(AttributeType.IntType())))
    )
  }

  "Semantic Analyser" should "parse each data type correctly" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        Entry.Attribute("a", Syntax.AttributeType("int", Args())),
        Entry.Attribute("b", Syntax.AttributeType("float", Args())),
        Entry.Attribute("c", Syntax.AttributeType("bool", Args())),
        Entry.Attribute("d", Syntax.AttributeType("date", Args())),
        Entry.Attribute("e", Syntax.AttributeType("time", Args())),
        Entry.Attribute("f", Syntax.AttributeType("datetime", Args())),
        Entry.Attribute("g", Syntax.AttributeType("data", Args())),
        Entry.Attribute("h", Syntax.AttributeType("string", Args()))
      )
    ) shouldEqual mkTemplefileSemanticsWithUserService(
      ServiceBlock(
        Map(
          "a" -> Attribute(AttributeType.IntType()),
          "b" -> Attribute(AttributeType.FloatType()),
          "c" -> Attribute(AttributeType.BoolType),
          "d" -> Attribute(AttributeType.DateType),
          "e" -> Attribute(AttributeType.TimeType),
          "f" -> Attribute(AttributeType.DateTimeType),
          "g" -> Attribute(AttributeType.BlobType()),
          "h" -> Attribute(AttributeType.StringType())
        )
      )
    )
  }
}
