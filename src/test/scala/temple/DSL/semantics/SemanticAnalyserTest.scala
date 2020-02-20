package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Analyser.parseSemantics
import temple.DSL.semantics.AttributeType._
import temple.DSL.syntax
import temple.DSL.syntax.Arg._
import temple.DSL.syntax.{Args, DSLRootItem, Entry}
import temple.DSL.syntax.Entry.{Attribute => AttributeEntry}
import SemanticAnalyserTest._

class SemanticAnalyserTest extends FlatSpec with Matchers {

  behavior of "Semantic Analyser"

  it should "complain that there is no project block when parsing an Empty AST" in {
    a[SemanticParsingException] should be thrownBy { parseSemantics(Nil) }
  }

  it should "parse an AST containing only an empty project block" in {
    parseSemantics(mkTemplefileAST()) shouldBe mkTemplefileSemantics()
  }

  it should "parse an AST containing a basic user service" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        AttributeEntry("index", syntax.AttributeType.Primitive("int")),
      ),
    ) shouldEqual mkTemplefileSemanticsWithUserService(
      ServiceBlock(Map("index" -> Attribute(IntType()))),
    )
  }

  it should "parse each data type correctly" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        AttributeEntry("a", syntax.AttributeType.Primitive("int")),
        AttributeEntry("b", syntax.AttributeType.Primitive("float")),
        AttributeEntry("c", syntax.AttributeType.Primitive("bool")),
        AttributeEntry("d", syntax.AttributeType.Primitive("date")),
        AttributeEntry("e", syntax.AttributeType.Primitive("time")),
        AttributeEntry("f", syntax.AttributeType.Primitive("datetime")),
        AttributeEntry("g", syntax.AttributeType.Primitive("data")),
        AttributeEntry("h", syntax.AttributeType.Primitive("string")),
        AttributeEntry("i", syntax.AttributeType.Foreign("User")),
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
          AttributeEntry("a", syntax.AttributeType.Primitive("bool")),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          AttributeEntry("a", syntax.AttributeType.Primitive("wat")),
        ),
      )
    }
  }

  it should "fail on too many arguments" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          AttributeEntry("a", syntax.AttributeType.Primitive("bool")),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          AttributeEntry("a", syntax.AttributeType.Primitive("bool", Args(Seq(IntArg(12))))),
        ),
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          AttributeEntry("a", syntax.AttributeType.Primitive("int", Args(Seq(IntArg(12), IntArg(12), IntArg(4))))),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          AttributeEntry(
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
          AttributeEntry("a", syntax.AttributeType.Primitive("int")),
          AttributeEntry("b", syntax.AttributeType.Primitive("float")),
        ),
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          AttributeEntry("a", syntax.AttributeType.Primitive("int")),
          AttributeEntry("a", syntax.AttributeType.Primitive("float")),
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
              AttributeEntry("a", syntax.AttributeType.Primitive("int")),
              AttributeEntry("b", syntax.AttributeType.Primitive("float")),
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
              AttributeEntry("a", syntax.AttributeType.Primitive("int")),
              AttributeEntry("a", syntax.AttributeType.Primitive("float")),
            ),
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
          AttributeEntry("a", syntax.AttributeType.Primitive("int"), Seq(syntax.Annotation("unique"))),
          AttributeEntry("b", syntax.AttributeType.Primitive("int"), Seq(syntax.Annotation("nullable"))),
          AttributeEntry("c", syntax.AttributeType.Primitive("float"), Seq(syntax.Annotation("serverSet"))),
          AttributeEntry("d", syntax.AttributeType.Primitive("float"), Seq(syntax.Annotation("client"))),
          AttributeEntry("e", syntax.AttributeType.Primitive("float"), Seq(syntax.Annotation("server"))),
        ),
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          AttributeEntry(
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
          AttributeEntry(
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
          AttributeEntry("a", syntax.AttributeType.Primitive("int"), Seq(syntax.Annotation("something"))),
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
              AttributeEntry("id", syntax.AttributeType.Primitive("int")),
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
          AttributeEntry("id", syntax.AttributeType.Primitive("int")),
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
          AttributeEntry("id", syntax.AttributeType.Primitive("int")),
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
        Seq(DSLRootItem("Test", "project", Seq(AttributeEntry("field", syntax.AttributeType.Primitive("int"))))),
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
          DSLRootItem("mobile", "target", Seq(AttributeEntry("field", syntax.AttributeType.Primitive("int")))),
        ),
      )
    }
  }
}

object SemanticAnalyserTest {

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
