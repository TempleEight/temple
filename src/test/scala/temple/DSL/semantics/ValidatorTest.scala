package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Validator.validate
import temple.DSL.semantics.ValidatorTest._
import temple.ast.Annotation
import temple.ast.Annotation.{Nullable, Unique}
import temple.ast.AttributeType._
import temple.ast.Metadata._
import temple.ast._

class ValidatorTest extends FlatSpec with Matchers {

  behavior of "Validator"

  it should "validate" in {
    noException shouldBe thrownBy {
      validate(
        Templefile(
          "MyProject",
          projectBlock = ProjectBlock(Seq(Database.Postgres, Provider.AWS)),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map(
                "a" -> Attribute(IntType(), accessAnnotation = Some(Annotation.Server)),
                "b" -> Attribute(BoolType, accessAnnotation = Some(Annotation.Client)),
                "c" -> Attribute(BlobType(), valueAnnotations = Set(Nullable, Unique)),
                "d" -> Attribute(FloatType(), accessAnnotation = Some(Annotation.ServerSet)),
                "e" -> Attribute(StringType()),
              ),
              metadata = Seq(ServiceAuth.Email, Writable.This, Readable.All, Database.Postgres, ServiceEnumerable()),
            ),
          ),
        ),
      )
    }

    the[SemanticParsingException] thrownBy {
      validate(
        Templefile(
          "MyProject",
          projectBlock = ProjectBlock(Seq(Database.Postgres)),
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
          projectBlock = ProjectBlock(Seq(Database.Postgres)),
          targets = Map("User" -> TargetBlock(Seq(TargetLanguage.JavaScript)), "Box" -> TargetBlock()),
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
          projectBlock = ProjectBlock(Seq(Database.Postgres)),
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
        templefileWithUserAttributes("A" -> Attribute(BoolType)),
      )
    } should have message "Invalid attribute name A, it must start with a lowercase letter, in User"

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
        templefileWithUserAttributes("a" -> Attribute(UUIDType)),
      )
    } should have message "Illegal use of UUID type in a, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("a" -> Attribute(BlobType(Some(-1)))),
      )
    } should have message "BlobType size is negative in a, in User"

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
        templefileWithUserAttributes("c" -> Attribute(StringType(max = Some(5), min = Some(8)))),
      )
    } should have message "StringType max not above min in c, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes("c" -> Attribute(ForeignKey("Unknown"))),
      )
    } should have message "Invalid foreign key Unknown in c, in User"

    the[SemanticParsingException] thrownBy {
      validate(
        Templefile(
          "MyProject",
          projectBlock = ProjectBlock(Seq(Database.Postgres)),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map(
                "a" -> Attribute(IntType()),
              ),
              metadata = Seq(Metadata.Uses(Seq("Box"))),
            ),
          ),
        ),
      )
    } should have message "No such service Box referenced in #uses in User"

    the[SemanticParsingException] thrownBy {
      validate(
        Templefile(
          "MyProject",
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map("a" -> Attribute(IntType())),
              metadata = Seq(Readable.All, Readable.This),
            ),
          ),
        ),
      )
    } should have message "Multiple occurrences of Readable metadata in User"

    the[SemanticParsingException] thrownBy {
      validate(
        Templefile(
          "MyProject",
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map("a" -> Attribute(IntType())),
              metadata = Seq(Writable.All, Readable.This),
            ),
          ),
        ),
      )
    } should have message "#writable(all) is not compatible with #readable(this)"
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
