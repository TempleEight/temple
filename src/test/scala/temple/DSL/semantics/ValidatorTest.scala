package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Validator.validate
import temple.DSL.semantics.ValidatorTest._
import temple.ast.AttributeType._
import temple.ast.{Attribute, Metadata, ProjectBlock, ServiceBlock, StructBlock, TargetBlock, Templefile}

class ValidatorTest extends FlatSpec with Matchers {

  behavior of "Validator"

  it should "validate" in {
    noException shouldBe thrownBy {
      validate(
        Templefile(
          "MyProject",
          projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres)),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map(
                "a" -> Attribute(IntType()),
                "b" -> Attribute(BoolType),
                "c" -> Attribute(FloatType()),
                "d" -> Attribute(StringType()),
              ),
            ),
          ),
        ),
      )
    }

    the[SemanticParsingException] thrownBy {
      validate(
        Templefile(
          "MyProject",
          projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres)),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map(
                "a" -> Attribute(IntType()),
              ),
              structs = Map(
                "User" -> StructBlock(Map()),
              ),
            ),
          ),
        ),
      )
    } should have message "Project, targets and structs must be globally unique, duplicate found: User (service, struct)"
    the[SemanticParsingException] thrownBy {
      validate(
        Templefile(
          "Box",
          projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres)),
          targets = Map("User" -> TargetBlock(), "Box" -> TargetBlock()),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map(
                "a" -> Attribute(IntType()),
              ),
              structs = Map(
                "User" -> StructBlock(Map()),
                "Box"  -> StructBlock(Map()),
              ),
            ),
          ),
        ),
      )
    } should have message "Project, targets and structs must be globally unique, duplicates found: Box (struct, target, project), User (service, struct, target)"

    the[SemanticParsingException] thrownBy {
      validate(
        Templefile("myProject"),
      )
    } should have message "Invalid name: myProject (project), it should start with a capital letter"

    the[SemanticParsingException] thrownBy {
      validate(
        Templefile(
          "User",
          projectBlock = ProjectBlock(Seq(Metadata.Database.Postgres)),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map(
                "a" -> Attribute(IntType()),
              ),
            ),
          ),
        ),
      )
    } should have message "Project, targets and structs must be globally unique, duplicate found: User (service, project)"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("a" -> Attribute(IntType(max = Some(0), min = Some(1)))),
      )
    } should have message "IntType max not above min in a, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("a" -> Attribute(IntType(precision = 16))),
      )
    } should have message "IntType precision not between 1 and 8 in a, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("b" -> Attribute(FloatType(max = Some(0), min = Some(1)))),
      )
    } should have message "FloatType max not above min in b, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("b" -> Attribute(FloatType(precision = -1))),
      )
    } should have message "FloatType precision not between 1 and 8 in b, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("c" -> Attribute(StringType(max = Some(-1)))),
      )
    } should have message "StringType max is negative in c, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("c" -> Attribute(ForeignKey("Unknown"))),
      )
    } should have message "Invalid foreign key Unknown in c, in User"
  }

}

object ValidatorTest {

  private def templefileWithUserAttributes(attributes: (String, Attribute)*) = Templefile(
    "TemplefileWithUserAttributes",
    services = Map(
      "User" -> ServiceBlock(attributes.toMap),
    ),
  )
}
