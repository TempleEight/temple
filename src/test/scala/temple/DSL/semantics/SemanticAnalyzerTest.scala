package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Analyzer.parseSemantics
import temple.ast.AttributeType._
import temple.DSL.syntax
import temple.DSL.syntax.Arg._
import temple.DSL.syntax.{Args, DSLRootItem, Entry}
import SemanticAnalyzerTest._
import temple.ast.{Attribute, ProjectBlock, ServiceBlock, Templefile}

class SemanticAnalyzerTest extends FlatSpec with Matchers {

  behavior of "Semantic Analyzer"

  it should "complain that there is no project block when parsing an Empty AST" in {
    a[SemanticParsingException] should be thrownBy { parseSemantics(Nil) }
  }

  it should "parse an AST containing only an empty project block" in {
    parseSemantics(mkTemplefileAST()) shouldBe mkTemplefileSemantics()
  }

  it should "parse an AST containing a basic user service" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        Entry.Attribute("index", syntax.AttributeType.Primitive("int")),
      ),
    ) shouldEqual mkTemplefileSemanticsWithUserService(
      ServiceBlock(Map("index" -> Attribute(IntType()))),
    )
  }

  it should "parse each data type correctly" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        Entry.Attribute("a", syntax.AttributeType.Primitive("int")),
        Entry.Attribute("b", syntax.AttributeType.Primitive("float")),
        Entry.Attribute("c", syntax.AttributeType.Primitive("bool")),
        Entry.Attribute("d", syntax.AttributeType.Primitive("date")),
        Entry.Attribute("e", syntax.AttributeType.Primitive("time")),
        Entry.Attribute("f", syntax.AttributeType.Primitive("datetime")),
        Entry.Attribute("g", syntax.AttributeType.Primitive("data")),
        Entry.Attribute("h", syntax.AttributeType.Primitive("string")),
        Entry.Attribute("i", syntax.AttributeType.Foreign("User")),
      ),
    ) shouldEqual mkTemplefileSemanticsWithUserService(
      ServiceBlock(
        Map(
          "a" -> Attribute(IntType()),
          "b" -> Attribute(FloatType()),
          "c" -> Attribute(BoolType),
          "d" -> Attribute(DateType),
          "e" -> Attribute(TimeType),
          "f" -> Attribute(DateTimeType),
          "g" -> Attribute(BlobType()),
          "h" -> Attribute(StringType()),
          "i" -> Attribute(ForeignKey("User")),
        ),
      ),
    )
  }

  it should "fail on an unknown type" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("bool")),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("wat")),
        ),
      )
    }
  }

  it should "fail on too many arguments" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("bool")),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("bool", Args(Seq(IntArg(12))))),
        ),
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int", Args(Seq(IntArg(12), IntArg(12), IntArg(4))))),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(Seq(IntArg(12), IntArg(12), IntArg(4), IntArg(12)))),
          ),
        ),
      )
    }
  }

  it should "fail on duplicate key names" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int")),
          Entry.Attribute("b", syntax.AttributeType.Primitive("float")),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int")),
          Entry.Attribute("a", syntax.AttributeType.Primitive("float")),
        ),
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem(
            "Neighbour",
            "struct",
            Seq(
              Entry.Attribute("a", syntax.AttributeType.Primitive("int")),
              Entry.Attribute("b", syntax.AttributeType.Primitive("float")),
            ),
          ),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem(
            "Neighbour",
            "struct",
            Seq(
              Entry.Attribute("a", syntax.AttributeType.Primitive("int")),
              Entry.Attribute("a", syntax.AttributeType.Primitive("float")),
            ),
          ),
        ),
      )
    }
  }

  it should "pass kwargs in any order" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(kwargs = Seq("max" -> IntArg(5), "min" -> IntArg(1)))),
          ),
        ),
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(kwargs = Seq("min" -> IntArg(1), "max" -> IntArg(5)))),
          ),
        ),
      )
    }
  }

  it should "fail to parse repeated kwargs" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(Seq(IntArg(5)), kwargs = Seq("max" -> IntArg(1)))),
          ),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(kwargs = Seq("max" -> IntArg(1), "max" -> IntArg(5)))),
          ),
        ),
      )
    }
  }

  it should "fail to parse a bad nested item in a service" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem("Neighbour", "badItem", Seq()),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem(
            "Neighbour",
            "struct",
            Seq(
              Entry.Metadata("language", Args(Seq(TokenArg("go")))),
            ),
          ),
        ),
      )
    }
  }

  it should "work with non-mutex annotations" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int"), Seq(syntax.Annotation("unique"))),
          Entry.Attribute("b", syntax.AttributeType.Primitive("int"), Seq(syntax.Annotation("nullable"))),
          Entry.Attribute("c", syntax.AttributeType.Primitive("float"), Seq(syntax.Annotation("serverSet"))),
          Entry.Attribute("d", syntax.AttributeType.Primitive("float"), Seq(syntax.Annotation("client"))),
          Entry.Attribute("e", syntax.AttributeType.Primitive("float"), Seq(syntax.Annotation("server"))),
        ),
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int"),
            Seq(syntax.Annotation("unique"), syntax.Annotation("server")),
          ),
        ),
      )
    }
  }

  it should "fail for mutually exclusive annotations" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int"),
            Seq(syntax.Annotation("client"), syntax.Annotation("server")),
          ),
        ),
      )
    }
  }

  it should "fail for unknown annotations" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int"), Seq(syntax.Annotation("something"))),
        ),
      )
    }
  }

  it should "accept valid annotations" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileAST(
          DSLRootItem(
            "User",
            "service",
            Seq(
              Entry.Attribute("id", syntax.AttributeType.Primitive("int")),
              Entry.Metadata("enumerable"),
              Entry.Metadata("uses", Args(kwargs = Seq("services" -> ListArg(Seq(TokenArg("Box")))))),
              Entry.Metadata("auth", Args(kwargs = Seq("login"    -> TokenArg("id")))),
            ),
          ),
          DSLRootItem("Box", "service", Seq()),
        ),
      )
    }
  }

  it should "fail on invalid annotations" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("id", syntax.AttributeType.Primitive("int")),
          Entry.Metadata("uses", Args(kwargs = Seq("badKey" -> ListArg(Seq())))),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Metadata("uses", Args(kwargs = Seq("services" -> IntArg(12)))),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Metadata("uses", Args(kwargs = Seq("services" -> NoArg))),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("id", syntax.AttributeType.Primitive("int")),
          Entry.Metadata("badMetadata"),
        ),
      )
    }
  }

  it should "parse project blocks correctly" in {
    noException should be thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq(Entry.Metadata("language", Args(Seq(TokenArg("go"))))))))
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq(Entry.Metadata("bad", Args(Seq(TokenArg("go"))))))))
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        Seq(DSLRootItem("Test", "project", Seq(Entry.Attribute("field", syntax.AttributeType.Primitive("int"))))),
      )
    }
  }

  it should "fail to parse duplicate project blocks" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq()), DSLRootItem("other", "project", Seq())))
    }
  }

  it should "fail to parse metadata without required parameter" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq(Entry.Metadata("language", Args())))))
    }
  }

  it should "fail to parse unknown root blocks" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq()), DSLRootItem("other", "badItem", Seq())))
    }
  }

  it should "parse target blocks correctly" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileAST(DSLRootItem("mobile", "target", Seq(Entry.Metadata("language", Args(Seq(TokenArg("swift"))))))),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileAST(DSLRootItem("mobile", "target", Seq(Entry.Metadata("badKey", Args(Seq(TokenArg("swift"))))))),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileAST(
          DSLRootItem("mobile", "target", Seq(Entry.Attribute("field", syntax.AttributeType.Primitive("int")))),
        ),
      )
    }
  }
}

object SemanticAnalyzerTest {

  private def mkTemplefileAST(rootItems: DSLRootItem*): syntax.Templefile =
    DSLRootItem("Test", "project", Nil) +: rootItems

  private def mkTemplefileSemantics(entries: (String, ServiceBlock)*): Templefile =
    Templefile("Test", ProjectBlock(), Map.empty, entries.toMap)

  private def mkTemplefileASTWithUserService(entries: Entry*): syntax.Templefile = Seq(
    DSLRootItem("Test", "project", Nil),
    DSLRootItem("User", "service", entries),
  )

  private def mkTemplefileSemanticsWithUserService(serviceBlock: ServiceBlock): Templefile =
    Templefile("Test", ProjectBlock(), Map.empty, Map("User" -> serviceBlock))
}