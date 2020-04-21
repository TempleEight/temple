package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.AbstractAttribute.Attribute
import temple.ast.Annotation.Unique
import temple.ast.AttributeType
import temple.ast.AttributeType._
import temple.ast.Metadata.{AuthMethod, Database, Readable, Writable}
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot.{ServiceRoot, StructRoot}
import temple.generate.server._

import scala.collection.immutable.{ListMap, SortedMap}

class ServerBuilderTest extends FlatSpec with Matchers {

  behavior of "ServerBuilderTest"

  it should "build a correct simple ServiceRoot" in {
    val serviceRoot: ServiceRoot = BuilderTestData.simpleTemplefile.allServicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            port = port.service,
            detail = GoLanguageDetail("github.com/squat/and/dab"),
            projectUsesAuth = false,
          )
    }
    serviceRoot shouldBe ServiceRoot(
      "TestService",
      "github.com/squat/and/dab/test-service",
      comms = Set("OtherService").map(ServiceName(_)),
      port = 1026,
      opQueries = SortedMap(
        List   -> "SELECT id, bank_balance, name, is_student, date_of_birth, time_of_day, expiry, image, fk FROM test_service;",
        Create -> "INSERT INTO test_service (id, bank_balance, name, is_student, date_of_birth, time_of_day, expiry, image, fk) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING id, bank_balance, name, is_student, date_of_birth, time_of_day, expiry, image, fk;",
        Read   -> "SELECT id, bank_balance, name, is_student, date_of_birth, time_of_day, expiry, image, fk FROM test_service WHERE id = $1;",
        Update -> "UPDATE test_service SET bank_balance = $1, name = $2, is_student = $3, date_of_birth = $4, time_of_day = $5, expiry = $6, image = $7, fk = $8 WHERE id = $9 RETURNING id, bank_balance, name, is_student, date_of_birth, time_of_day, expiry, image, fk;",
        Delete -> "DELETE FROM test_service WHERE id = $1;",
      ),
      idAttribute = IDAttribute("id"),
      createdByAttribute = None,
      attributes = ListMap(
        "bankBalance" -> Attribute(FloatType()),
        "name"        -> Attribute(StringType()),
        "isStudent"   -> Attribute(BoolType),
        "dateOfBirth" -> Attribute(DateType),
        "timeOfDay"   -> Attribute(TimeType),
        "expiry"      -> Attribute(DateTimeType),
        "image"       -> Attribute(BlobType()),
        "fk"          -> Attribute(ForeignKey("OtherService")),
      ),
      structs = Nil,
      datastore = Database.Postgres,
      readable = Readable.All,
      writable = Writable.All,
      projectUsesAuth = false,
      hasAuthBlock = false,
      metrics = None,
    )
  }

  it should "build a correct complex ServiceRoot" in {
    val serviceRoot: ServiceRoot = BuilderTestData.complexTemplefile.allServicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            port = port.service,
            detail = GoLanguageDetail("github.com/squat/and/dab"),
            projectUsesAuth = false,
          )
    }
    serviceRoot shouldBe ServiceRoot(
      "TestComplexService",
      "github.com/squat/and/dab/test-complex-service",
      comms = Set("OtherSvc").map(ServiceName(_)),
      port = 1026,
      opQueries = SortedMap(
        List   -> "SELECT id, another_id, yet_another_id, bank_balance, big_bank_balance, name, initials, is_student, date_of_birth, time_of_day, expiry, image, sample_fk1, sample_fk2 FROM test_complex_service;",
        Create -> "INSERT INTO test_complex_service (id, another_id, yet_another_id, bank_balance, big_bank_balance, name, initials, is_student, date_of_birth, time_of_day, expiry, image, sample_fk1, sample_fk2) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14) RETURNING id, another_id, yet_another_id, bank_balance, big_bank_balance, name, initials, is_student, date_of_birth, time_of_day, expiry, image, sample_fk1, sample_fk2;",
        Read   -> "SELECT id, another_id, yet_another_id, bank_balance, big_bank_balance, name, initials, is_student, date_of_birth, time_of_day, expiry, image, sample_fk1, sample_fk2 FROM test_complex_service WHERE id = $1;",
        Update -> "UPDATE test_complex_service SET another_id = $1, yet_another_id = $2, bank_balance = $3, big_bank_balance = $4, name = $5, initials = $6, is_student = $7, date_of_birth = $8, time_of_day = $9, expiry = $10, image = $11, sample_fk1 = $12, sample_fk2 = $13 WHERE id = $14 RETURNING id, another_id, yet_another_id, bank_balance, big_bank_balance, name, initials, is_student, date_of_birth, time_of_day, expiry, image, sample_fk1, sample_fk2;",
        Delete -> "DELETE FROM test_complex_service WHERE id = $1;",
      ),
      idAttribute = IDAttribute("id"),
      createdByAttribute = None,
      attributes = ListMap(
        "anotherId"      -> Attribute(IntType(max = Some(100), min = Some(10))),
        "yetAnotherId"   -> Attribute(IntType(max = Some(100), min = Some(10), precision = 8)),
        "bankBalance"    -> Attribute(FloatType(max = Some(300), min = Some(0), precision = 4)),
        "bigBankBalance" -> Attribute(FloatType(max = Some(123), min = Some(0))),
        "name"           -> Attribute(StringType(max = None, min = Some(1))),
        "initials"       -> Attribute(StringType(max = Some(5), min = Some(0))),
        "isStudent"      -> Attribute(BoolType),
        "dateOfBirth"    -> Attribute(DateType),
        "timeOfDay"      -> Attribute(TimeType),
        "expiry"         -> Attribute(DateTimeType),
        "image"          -> Attribute(BlobType()),
        "sampleFK1"      -> Attribute(ForeignKey("OtherSvc")),
        "sampleFK2"      -> Attribute(ForeignKey("OtherSvc")),
      ),
      structs = Seq(
        StructRoot(
          name = "Test",
          opQueries = SortedMap(
            Create -> "INSERT INTO test (favourite_colour, bed_time, favourite_number) VALUES ($1, $2, $3) RETURNING favourite_colour, bed_time, favourite_number;",
            Read   -> "SELECT favourite_colour, bed_time, favourite_number FROM test WHERE id = $1;",
            Update -> "UPDATE test SET favourite_colour = $1, bed_time = $2, favourite_number = $3 WHERE id = $4 RETURNING favourite_colour, bed_time, favourite_number;",
            Delete -> "DELETE FROM test WHERE id = $1;",
          ),
          idAttribute = IDAttribute("id"),
          createdByAttribute = None,
          parentAttribute = Some(ParentAttribute("parentID")),
          attributes = ListMap(
            "favouriteColour" -> Attribute(StringType(None, None), None, Set(Unique)),
            "bedTime"         -> Attribute(TimeType, None, Set()),
            "favouriteNumber" -> Attribute(IntType(Some(10), Some(0), 4), None, Set()),
          ),
          readable = Readable.All,
          writable = Writable.All,
          projectUsesAuth = false,
        ),
      ),
      datastore = Database.Postgres,
      readable = Readable.All,
      writable = Writable.All,
      projectUsesAuth = false,
      hasAuthBlock = false,
      metrics = None,
    )
  }

  it should "generate correct AuthServiceRoot" in {
    val authServiceRoot: AuthServiceRoot = ServerBuilder.buildAuthRoot(
      BuilderTestData.simpleTemplefile,
      GoLanguageDetail("github.com/squat/and/dab"),
      1000,
    )

    authServiceRoot shouldBe AuthServiceRoot(
      "github.com/squat/and/dab/auth",
      1000,
      AuthAttribute(AuthMethod.Email, AttributeType.StringType()),
      IDAttribute("id"),
      "INSERT INTO auth (id, email, password) VALUES ($1, $2, $3) RETURNING id, email, password;",
      "SELECT id, email, password FROM auth WHERE email = $1;",
      metrics = None,
    )
  }

}
