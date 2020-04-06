package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Analyzer.parseSemantics
import temple.DSL.semantics.SemanticAnalyzerTest._
import temple.DSL.syntax
import temple.DSL.syntax.Arg._
import temple.DSL.syntax.{Args, DSLRootItem, Entry}
import temple.ast.AbstractServiceBlock.ServiceBlock
import temple.ast.AttributeType._
import temple.ast.{Attribute, Templefile}

class SemanticAnalyzerTest extends FlatSpec with Matchers {

  behavior of "Semantic Analyzer"

  it should "complain that there is no project block when parsing an Empty AST" in {
    the[SemanticParsingException] thrownBy parseSemantics(Nil) should have message "Temple file has no project block"
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

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("wat")),
        ),
      )
    } should have message "Unknown type wat in a, in User service"
  }

  it should "fail on too many arguments" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("bool")),
        ),
      )
    }

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("bool", Args(Seq(IntArg(12))))),
        ),
      )
    } should have message "Arguments supplied to function that should take no parameters in bool, in a, in User service"

    noException should be thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int", Args(Seq(IntArg(12), IntArg(12), IntArg(4))))),
        ),
      )
    }

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(Seq(IntArg(12), IntArg(12), IntArg(4), IntArg(12)))),
          ),
        ),
      )
    } should have message "Too many arguments supplied to function (found 4, expected at most 3) in int, in a, in User service"
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

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int")),
          Entry.Attribute("a", syntax.AttributeType.Primitive("float")),
        ),
      )
    } should have message "Key a already exists in LinkedHashMap(a -> Attribute(IntType(None,None,4),None,Set())) in User service"

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

    the[SemanticParsingException] thrownBy {
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

    } should have message "Key a already exists in LinkedHashMap(a -> Attribute(IntType(None,None,4),None,Set())) in Neighbour struct, in User service"
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
    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(Seq(IntArg(5)), kwargs = Seq("max" -> IntArg(1)))),
          ),
        ),
      )
    } should have message "Duplicate argument provided for max in int, in a, in User service"

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int", Args(kwargs = Seq("max" -> IntArg(1), "max" -> IntArg(5)))),
          ),
        ),
      )

    } should have message "Duplicate argument provided for max in int, in a, in User service"
  }

  it should "fail to parse a bad nested item in a service" in {
    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          DSLRootItem("Neighbour", "badItem", Seq()),
        ),
      )
    } should have message "Unknown block type badItem for Neighbour in User service"

    the[SemanticParsingException] thrownBy {
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

    } should have message "No valid metadata language in Neighbour struct, in User service"
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
    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute(
            "a",
            syntax.AttributeType.Primitive("int"),
            Seq(syntax.Annotation("client"), syntax.Annotation("server")),
          ),
        ),
      )
    } should have message "Two scope annotations found: @server is incompatible with @client in a, in User service"
  }

  it should "fail for unknown annotations" in {
    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("a", syntax.AttributeType.Primitive("int"), Seq(syntax.Annotation("something"))),
        ),
      )
    } should have message "Unknown annotation @something in a, in User service"
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
              Entry.Metadata("auth", Args(Seq(TokenArg("email")))),
            ),
          ),
          DSLRootItem("Box", "service", Seq()),
        ),
      )
    }
  }

  it should "fail on invalid annotations" in {
    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("id", syntax.AttributeType.Primitive("int")),
          Entry.Metadata("uses", Args(kwargs = Seq("badKey" -> ListArg(Seq())))),
        ),
      )
    } should have message "Unknown keyword argument badKey with value ListArg(List()) in uses, in User service"

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Metadata("uses", Args(kwargs = Seq("services" -> IntArg(12)))),
        ),
      )
    } should have message "Token list expected at services, found IntArg(12), in uses, in User service"

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Metadata("uses", Args(kwargs = Seq("services" -> NoArg))),
        ),
      )
    } should have message "Token list expected at services, found NoArg, in uses, in User service"

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("id", syntax.AttributeType.Primitive("int")),
          Entry.Metadata("badMetadata"),
        ),
      )
    } should have message "No valid metadata badMetadata in User service"

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileASTWithUserService(
          Entry.Attribute("id", syntax.AttributeType.Primitive("int")),
          DSLRootItem(
            "Fred",
            "struct",
            Seq(
              Entry.Metadata("badMetadata"),
            ),
          ),
        ),
      )
    } should have message "No valid metadata badMetadata in Fred struct, in User service"
  }

  it should "parse project blocks correctly" in {
    noException should be thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq(Entry.Metadata("language", Args(Seq(TokenArg("go"))))))))
    }

    the[SemanticParsingException] thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq(Entry.Metadata("bad", Args(Seq(TokenArg("go"))))))))
    } should have message "No valid metadata bad in Test project"

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        Seq(DSLRootItem("Test", "project", Seq(Entry.Attribute("field", syntax.AttributeType.Primitive("int"))))),
      )
    } should have message "Found unexpected attribute: `field: int;` in Test project"
  }

  it should "fail to parse duplicate project blocks" in {
    the[SemanticParsingException] thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq()), DSLRootItem("other", "project", Seq())))
    } should have message "Second project found in addition to Test, in other project"
  }

  it should "fail to parse metadata without required parameter" in {
    the[SemanticParsingException] thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq(Entry.Metadata("language", Args())))))
    } should have message "Required argument language not provided in language, in Test project"
  }

  it should "fail to parse unknown root blocks" in {
    the[SemanticParsingException] thrownBy {
      parseSemantics(Seq(DSLRootItem("Test", "project", Seq()), DSLRootItem("other", "badItem", Seq())))
    } should have message "Unknown block type in other badItem"
  }

  it should "parse target blocks correctly" in {
    noException should be thrownBy {
      parseSemantics(
        mkTemplefileAST(DSLRootItem("mobile", "target", Seq(Entry.Metadata("language", Args(Seq(TokenArg("swift"))))))),
      )
    }

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileAST(DSLRootItem("mobile", "target", Seq(Entry.Metadata("badKey", Args(Seq(TokenArg("swift"))))))),
      )
    } should have message "No valid metadata badKey in mobile target"

    the[SemanticParsingException] thrownBy {
      parseSemantics(
        mkTemplefileAST(
          DSLRootItem("mobile", "target", Seq(Entry.Attribute("field", syntax.AttributeType.Primitive("int")))),
        ),
      )
    } should have message "Found unexpected attribute: `field: int;` in mobile target"
  }
}

object SemanticAnalyzerTest {

  private def mkTemplefileAST(rootItems: DSLRootItem*): syntax.Templefile =
    DSLRootItem("Test", "project", Nil) +: rootItems

  private def mkTemplefileSemantics(entries: (String, ServiceBlock)*): Templefile =
    Templefile("Test", services = entries.toMap)

  private def mkTemplefileASTWithUserService(entries: Entry*): syntax.Templefile = Seq(
    DSLRootItem("Test", "project", Nil),
    DSLRootItem("User", "service", entries),
  )

  private def mkTemplefileSemanticsWithUserService(serviceBlock: ServiceBlock): Templefile =
    Templefile("Test", services = Map("User" -> serviceBlock))
}
