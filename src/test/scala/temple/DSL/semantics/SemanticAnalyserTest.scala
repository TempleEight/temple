package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.Syntax
import temple.DSL.Syntax.Arg._
import temple.DSL.Syntax.{Args, DSLRootItem, Entry}
import temple.DSL.semantics.Analyser.parseSemantics
import temple.DSL.semantics.AttributeType._

class SemanticAnalyserTest extends FlatSpec with Matchers {

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

  "Semantic Analyser" should "complain that there is no project block when parsing an Empty AST" in {
    a[SemanticParsingException] should be thrownBy { parseSemantics(Nil) }
  }

  it should "parse an AST containing only an empty project block" in {
    parseSemantics(mkTemplefileAST()) shouldBe mkTemplefileSemantics()
  }

  it should "parse an AST containing a basic user service" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        Entry.Attribute("index", Syntax.AttributeType("int"))
      )
    ) shouldEqual mkTemplefileSemanticsWithUserService(
      ServiceBlock(Map("index" -> Attribute(IntType())))
    )
  }

  it should "parse each data type correctly" in {
    parseSemantics(
      mkTemplefileASTWithUserService(
        Entry.Attribute("a", Syntax.AttributeType("int")),
        Entry.Attribute("b", Syntax.AttributeType("float")),
        Entry.Attribute("c", Syntax.AttributeType("bool")),
        Entry.Attribute("d", Syntax.AttributeType("date")),
        Entry.Attribute("e", Syntax.AttributeType("time")),
        Entry.Attribute("f", Syntax.AttributeType("datetime")),
        Entry.Attribute("g", Syntax.AttributeType("data")),
        Entry.Attribute("h", Syntax.AttributeType("string"))
      )
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
          "h" -> Attribute(StringType())
        )
      )
    )
  }

  it should "fail on too many arguments" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("bool"))
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("bool", Args(Seq(IntArg(12)))))
        )
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("int", Args(Seq(IntArg(12), IntArg(12)))))
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("intArgs", Args(Seq(IntArg(12), IntArg(12), IntArg(12)))))
        )
      )
    }
  }

  it should "fail on duplicate key names" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("int")),
          Entry.Attribute("b", Syntax.AttributeType("float"))
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("int")),
          Entry.Attribute("a", Syntax.AttributeType("float"))
        )
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem(
            "Neighbour",
            "struct",
            Seq(
              Entry.Attribute("a", Syntax.AttributeType("int")),
              Entry.Attribute("b", Syntax.AttributeType("float"))
            )
          )
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem(
            "Neighbour",
            "struct",
            Seq(
              Entry.Attribute("a", Syntax.AttributeType("int")),
              Entry.Attribute("a", Syntax.AttributeType("float"))
            )
          )
        )
      )
    }
  }

  it should "fail to parse a bad nested item in a service" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem("Neighbour", "badItem", Seq())
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem(
            "Neighbour",
            "struct",
            Seq(
              Entry.Metadata("language", Args(Seq(TokenArg("go"))))
            )
          )
        )
      )
    }
  }

  it should "work with non-mutex annotations" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("int"), Seq(Syntax.Annotation("unique"))),
          Entry.Attribute("b", Syntax.AttributeType("float"), Seq(Syntax.Annotation("serverSet"))),
          Entry.Attribute("c", Syntax.AttributeType("float"), Seq(Syntax.Annotation("client"))),
          Entry.Attribute("d", Syntax.AttributeType("float"), Seq(Syntax.Annotation("server")))
        )
      )
    }

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            Syntax.AttributeType("int"),
            Seq(Syntax.Annotation("unique"), Syntax.Annotation("server"))
          )
        )
      )
    }
  }

  it should "fail for mutually exclusive annotations" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            Syntax.AttributeType("int"),
            Seq(Syntax.Annotation("client"), Syntax.Annotation("server"))
          )
        )
      )
    }
  }

  it should "fail for unknown annotations" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", Syntax.AttributeType("int"), Seq(Syntax.Annotation("something")))
        )
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
              Entry.Attribute("id", Syntax.AttributeType("int")),
              Entry.Metadata("uses", Args(kwargs = Seq("services" -> ListArg(Seq(TokenArg("Box")))))),
              Entry.Metadata("auth", Args(kwargs = Seq("login"    -> TokenArg("id"))))
            )
          ),
          DSLRootItem("Box", "service", Seq())
        )
      )
    }
  }

  it should "fail on invalid annotations" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("id", Syntax.AttributeType("int")),
          Entry.Metadata("uses", Args(kwargs = Seq("badKey" -> ListArg(Seq()))))
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Metadata("uses", Args(kwargs = Seq("services" -> IntArg(12))))
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Metadata("uses", Args(kwargs = Seq("services" -> NoArg)))
        )
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("id", Syntax.AttributeType("int")),
          Entry.Metadata("badMetadata")
        )
      )
    }
  }

  it should "parse project blocks correctly" in {
    noException should be thrownBy {
      parseSemantics(Seq(DSLRootItem("test", "project", Seq(Entry.Metadata("language", Args(Seq(TokenArg("go"))))))))
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("test", "project", Seq(Entry.Metadata("bad", Args(Seq(TokenArg("go"))))))))
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("test", "project", Seq(Entry.Attribute("field", Syntax.AttributeType("int"))))))
    }
  }

  it should "fail to parse duplicate project blocks" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("test", "project", Seq()), DSLRootItem("other", "project", Seq())))
    }
  }

  it should "fail to parse metadata without required parameter" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("test", "project", Seq(Entry.Metadata("language", Args())))))
    }
  }

  it should "fail to parse unknown root blocks" in {
    a[SemanticParsingException] should be thrownBy {
      parseSemantics(Seq(DSLRootItem("test", "project", Seq()), DSLRootItem("other", "badItem", Seq())))
    }
  }

  it should "parse target blocks correctly" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileAST(DSLRootItem("mobile", "target", Seq(Entry.Metadata("language", Args(Seq(TokenArg("swift")))))))
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileAST(DSLRootItem("mobile", "target", Seq(Entry.Metadata("badKey", Args(Seq(TokenArg("swift")))))))
      )
    }

    a[SemanticParsingException] should be thrownBy {
      parseSemantics(
        mkTemplefileAST(DSLRootItem("mobile", "target", Seq(Entry.Attribute("field", Syntax.AttributeType("int")))))
      )
    }
  }
}
