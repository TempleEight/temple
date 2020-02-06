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
    ) shouldEqual mkTemplefileSemanticsWithUserService(ServiceBlock(Map("index" -> Attribute(AttributeType.IntType()))))
  }
}
