package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Validator._
import temple.DSL.semantics.ValidatorTest._
import temple.ast.AbstractServiceBlock._
import temple.ast.Annotation.{Nullable, Unique}
import temple.ast.AttributeType._
import temple.ast.Metadata.Endpoint.Delete
import temple.ast.Metadata._
import temple.ast.{Annotation, _}

class ValidatorTest extends FlatSpec with Matchers {

  behavior of "Validator"

  it should "validationErrors" in {
    validationErrors(
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
              "f" -> Attribute(ForeignKey("Box")),
            ),
            metadata = Seq(
              ServiceAuth.Email,
              Writable.This,
              Readable.All,
              Database.Postgres,
              ServiceEnumerable,
              ServiceLanguage.Go,
              Omit(Set(Delete)),
            ),
          ),
          "Box" -> ServiceBlock(Map()),
        ),
      ),
    ) shouldBe empty

    validationErrors(
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
    ) shouldBe Set(
      "Project, targets and structs must be globally unique, duplicate found: User (service, struct)",
    )

    validationErrors(
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
    ) shouldBe Set(
      "Project, targets and structs must be globally unique, duplicates found: Box (struct, target, project), User (service, struct, target)",
    )

    validationErrors(
      Templefile("myProject"),
    ) shouldBe Set("Invalid name: myProject (project), it should start with a capital letter")

    validationErrors(
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
    ) shouldBe Set(
      "Project, targets and structs must be globally unique, duplicate found: User (service, project)",
    )

    validationErrors(
      Templefile(
        "User",
        projectBlock = ProjectBlock(Seq(Metrics.Prometheus, Metrics.Prometheus)),
      ),
    ) shouldBe Set(
      "Multiple occurrences of Metrics metadata in User project",
    )

    validationErrors(
      templefileWithUserAttributes("A" -> Attribute(BoolType)),
    ) shouldBe Set("Invalid attribute name A, it must start with a lowercase letter, in User")

    validationErrors(
      templefileWithUserAttributes("a" -> Attribute(IntType(max = Some(0), min = Some(1)))),
    ) shouldBe Set("IntType max not above min in a, in User")

    validationErrors(
      templefileWithUserAttributes("a" -> Attribute(IntType(precision = 16))),
    ) shouldBe Set("IntType precision not between 1 and 8 in a, in User")

    validationErrors(
      templefileWithUserAttributes("a" -> Attribute(UUIDType)),
    ) shouldBe Set("Illegal use of UUID type in a, in User")

    validationErrors(
      templefileWithUserAttributes("a" -> Attribute(BlobType(Some(-1)))),
    ) shouldBe Set("BlobType size is negative in a, in User")

    validationErrors(
      templefileWithUserAttributes(
        "b" -> Attribute(FloatType(max = Some(0), min = Some(1))),
        "c" -> Attribute(FloatType(precision = -1)),
      ),
    ) shouldBe Set(
      "FloatType max not above min in b, in User",
      "FloatType precision not between 1 and 8 in c, in User",
    )

    validationErrors(
      templefileWithUserAttributes(
        "c" -> Attribute(StringType(max = Some(-1))),
        "d" -> Attribute(StringType(max = Some(5), min = Some(8))),
      ),
    ) shouldBe Set(
      "StringType max is negative in c, in User",
      "StringType max not above min in d, in User",
    )

    validationErrors(
      templefileWithUserAttributes("c" -> Attribute(ForeignKey("Unknown"))),
    ) shouldBe Set("Invalid foreign key Unknown in c, in User")

    validationErrors(
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
    ) shouldBe Set("No such service Box referenced in #uses in User")

    validationErrors(
      Templefile(
        "MyProject",
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(Readable.All, Readable.This),
          ),
        ),
      ),
    ) shouldBe Set("Multiple occurrences of Readable metadata in User")

    validationErrors(
      Templefile(
        "MyProject",
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(Writable.All, Readable.This),
          ),
        ),
      ),
    ) shouldBe Set("#writable(all) is not compatible with #readable(this) in User")
  }

  it should "validate" in {
    noException shouldBe thrownBy {
      validate(
        Templefile(
          "MyProject",
          projectBlock = ProjectBlock(Seq(Database.Postgres, Provider.AWS)),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map("a" -> Attribute(IntType())),
              metadata = Seq(ServiceAuth.Email),
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
    } should have message {
      """An error was encountered while validating the Templefile
        |Project, targets and structs must be globally unique, duplicate found: User (service, struct)""".stripMargin
    }

    the[SemanticParsingException] thrownBy {
      validate(
        templefileWithUserAttributes(
          "b" -> Attribute(FloatType(max = Some(0), min = Some(1))),
          "c" -> Attribute(FloatType(precision = -1)),
        ),
      )
    } should have message {
      s"""2 errors were encountered while validating the Templefile
         |FloatType max not above min in b, in User
         |FloatType precision not between 1 and 8 in c, in User""".stripMargin
    }
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
