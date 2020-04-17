package temple.DSL.semantics

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Validator._
import temple.DSL.semantics.ValidatorTest._
import temple.ast.AbstractAttribute.{Attribute, IDAttribute}
import temple.ast.AbstractServiceBlock._
import temple.ast.Annotation.{Nullable, Unique}
import temple.ast.AttributeType._
import temple.ast.Metadata.Endpoint.Delete
import temple.ast.Metadata._
import temple.ast._

class ValidatorTest extends FlatSpec with Matchers {

  behavior of "Validator"

  it should "throw no errors with a correct templefile" in {
    validationErrors(
      Templefile(
        "MyProject",
        projectBlock = ProjectBlock(Seq(Database.Postgres, Provider.Kubernetes, AuthMethod.Email)),
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
              ServiceAuth,
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
  }

  it should "identify duplicate usage of structs" in {
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
        targets = Map("User" -> TargetBlock(Seq(TargetLanguage.JavaScript))),
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
          "Other" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            structs = Map(
              "User" -> StructBlock(Map()),
            ),
          ),
        ),
      ),
    ) shouldBe Set(
      "Project, targets and structs must be globally unique, duplicates found: Box (project, struct), User (service, struct, target)",
    )

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
      "Project, targets and structs must be globally unique, duplicate found: User (project, service)",
    )
  }

  it should "enforce capitalization of project names" in {
    validationErrors(
      Templefile("myProject"),
    ) shouldBe Set("Invalid name: myProject (project), it should start with a capital letter")
  }

  it should "identify repeated use of metadata" in {
    validationErrors(
      Templefile(
        "User",
        projectBlock = ProjectBlock(Seq(Metrics.Prometheus, Metrics.Prometheus)),
      ),
    ) shouldBe Set(
      "Multiple occurrences of Metrics metadata in User project",
    )

    validationErrors(
      Templefile(
        "MyProject",
        projectBlock = ProjectBlock(Seq(AuthMethod.Email)),
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(ServiceAuth, Readable.All, Readable.This),
          ),
        ),
      ),
    ) shouldBe Set("Multiple occurrences of Readable metadata in User")
  }

  it should "enforce capitalization of attributes" in {
    validationErrors(
      templefileWithUserAttributes("A" -> Attribute(BoolType)),
    ) shouldBe Set("Invalid attribute name A, it must start with a lowercase letter, in User")
  }

  it should "validate constraints on attributes" in {
    validationErrors(
      templefileWithUserAttributes("a" -> Attribute(IntType(max = Some(0), min = Some(1)))),
    ) shouldBe Set("IntType max not above min in a, in User")

    validationErrors(
      templefileWithUserAttributes(
        "a" -> Attribute(IntType(min = Some(1024), precision = 1)),
        "b" -> Attribute(IntType(max = Some(-50_000), precision = 2)),
      ),
    ) shouldBe Set(
      "IntType min is out of range for the precision 1 in a, in User",
      "IntType max is out of range for the precision 2 in b, in User",
    )

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
  }

  it should "identify unknown foreign keys" in {
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
  }

  it should "enforce the existence of an auth block when other dependent metadata are used" in {
    validationErrors(
      Templefile(
        "MyProject",
        projectBlock = ProjectBlock(Seq(AuthMethod.Email)),
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(ServiceAuth),
          ),
          "Box" -> ServiceBlock(
            attributes = Map(),
            metadata = Seq(Readable.This),
          ),
        ),
      ),
    ) shouldBe Set()

    validationErrors(
      Templefile(
        "MyProject",
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(Readable.This, Writable.This),
          ),
        ),
      ),
    ) shouldBe Set(
      "#readable(this) requires an #authMethod to be declared for the project in User",
      "#writable(this) requires an #authMethod to be declared for the project in User",
    )
  }

  it should "identify incompatible metadata" in {
    validationErrors(
      Templefile(
        "MyProject",
        projectBlock = ProjectBlock(Seq(AuthMethod.Email)),
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(ServiceAuth, Writable.All, Readable.This),
          ),
        ),
      ),
    ) shouldBe Set("#writable(all) is not compatible with #readable(this) in User")
  }

  it should "identify #auth xor #authMethod being present" in {
    validationErrors(
      Templefile(
        "MyProject",
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(ServiceAuth),
          ),
        ),
      ),
    ) shouldBe Set("#auth requires an #authMethod to be declared for the project in User")

    validationErrors(
      Templefile(
        "MyProject",
        projectBlock = ProjectBlock(Seq(AuthMethod.Email)),
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
          ),
        ),
      ),
    ) shouldBe Set("#authMethod requires at least one block to have #auth declared in MyProject project")

    validationErrors(
      Templefile(
        "MyProject",
        projectBlock = ProjectBlock(Seq(AuthMethod.Email)),
        services = Map(
          "User" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
            metadata = Seq(ServiceAuth),
          ),
        ),
      ),
    ) shouldBe empty

    validationErrors(
      Templefile(
        "MyProject",
        services = Map(
          "User" -> ServiceBlock(attributes = Map("a" -> Attribute(IntType()))),
        ),
      ),
    ) shouldBe empty
  }

  it should "find cycles in dependencies, but only if they are not nullable" in {
    validationErrors(
      Templefile(
        "MyProject",
        services = Map(
          "User" -> ServiceBlock(Map("box"  -> Attribute(ForeignKey("Box")))),
          "Box"  -> ServiceBlock(Map("fred" -> Attribute(ForeignKey("Fred"), None, Set(Nullable)))),
          "Fred" -> ServiceBlock(Map("user" -> Attribute(ForeignKey("User")))),
        ),
      ),
    ) shouldBe Set()

    validationErrors(
      Templefile(
        "MyProject",
        services = Map(
          "User"   -> ServiceBlock(Map("box"    -> Attribute(ForeignKey("Box")))),
          "Box"    -> ServiceBlock(Map("cube"   -> Attribute(ForeignKey("Cube")))),
          "Cube"   -> ServiceBlock(Map("square" -> Attribute(ForeignKey("Square")))),
          "Square" -> ServiceBlock(Map("user"   -> Attribute(ForeignKey("User")))),
        ),
      ),
    ) shouldBe Set("Cycle(s) were detected in foreign keys, between elements: { Box, Cube, Square, User }")
  }

  it should "throw errors when validating" in {
    noException shouldBe thrownBy {
      validate(
        Templefile(
          "MyProject",
          projectBlock = ProjectBlock(Seq(Database.Postgres, Provider.Kubernetes, AuthMethod.Email)),
          services = Map(
            "User" -> ServiceBlock(
              attributes = Map("a" -> Attribute(IntType())),
              metadata = Seq(ServiceAuth),
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

  it should "validate to the correct result" in {
    validate(
      Templefile(
        "Fleet",
        projectBlock = ProjectBlock(),
        services = Map(
          "Driver" -> ServiceBlock(
            attributes = Map("a" -> Attribute(IntType())),
          ),
          "Car" -> ServiceBlock(attributes = Map()),
          "Van" -> ServiceBlock(
            attributes = Map(),
            metadata = Seq(Omit(Set(Endpoint.Update, Endpoint.Delete))),
          ),
          "Bus" -> ServiceBlock(
            attributes = Map(),
            metadata = Seq(Omit(Set(Endpoint.Delete))),
          ),
        ),
      ),
    ) shouldBe {
      Templefile(
        "Fleet",
        projectBlock = ProjectBlock(),
        services = Map(
          "Driver" -> ServiceBlock(
            attributes = Map("id" -> IDAttribute, "a" -> Attribute(IntType())),
          ),
          "Car" -> ServiceBlock(
            attributes = Map("id" -> IDAttribute),
            metadata = Seq(Omit(Set(Endpoint.Update))),
          ),
          "Van" -> ServiceBlock(
            attributes = Map("id" -> IDAttribute),
            metadata = Seq(Omit(Set(Endpoint.Update, Endpoint.Delete))),
          ),
          "Bus" -> ServiceBlock(
            attributes = Map("id" -> IDAttribute),
            metadata = Seq(Omit(Set(Endpoint.Update, Endpoint.Delete))),
          ),
        ),
      )
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
