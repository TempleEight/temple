package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.AbstractAttribute.Attribute
import temple.ast.AttributeType
import temple.ast.AttributeType._
import temple.ast.Metadata.{Database, Readable, ServiceAuth, Writable}
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server._

import scala.collection.immutable.{ListMap, SortedMap}

class ServerBuilderTest extends FlatSpec with Matchers {

  behavior of "ServerBuilderTest"

  it should "build a correct simple ServiceRoot with all endpoints" in {
    val serviceRoot: ServiceRoot = BuilderTestData.simpleTemplefile.allServicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            endpoints = Set(Create, Read, Update, Delete, List),
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
        Update -> "UPDATE test_service SET id = $1, bank_balance = $2, name = $3, is_student = $4, date_of_birth = $5, time_of_day = $6, expiry = $7, image = $8, fk = $9 WHERE id = $10 RETURNING id, bank_balance, name, is_student, date_of_birth, time_of_day, expiry, image, fk;",
        Delete -> "DELETE FROM test_service WHERE id = $1;",
      ),
      idAttribute = IDAttribute("id"),
      createdByAttribute = None,
      attributes = ListMap(
        "id"          -> Attribute(IntType()),
        "bankBalance" -> Attribute(FloatType()),
        "name"        -> Attribute(StringType()),
        "isStudent"   -> Attribute(BoolType),
        "dateOfBirth" -> Attribute(DateType),
        "timeOfDay"   -> Attribute(TimeType),
        "expiry"      -> Attribute(DateTimeType),
        "image"       -> Attribute(BlobType()),
        "fk"          -> Attribute(ForeignKey("OtherService")),
      ),
      datastore = Database.Postgres,
      readable = Readable.All,
      writable = Writable.All,
      projectUsesAuth = false,
      hasAuthBlock = false,
      metrics = None,
    )
  }

  it should "build a correct simple ServiceRoot with no endpoints" in {
    val serviceRoot: ServiceRoot = BuilderTestData.simpleTemplefile.allServicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            endpoints = Set(),
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
      opQueries = SortedMap(),
      idAttribute = IDAttribute("id"),
      createdByAttribute = None,
      attributes = ListMap(
        "id"          -> Attribute(IntType()),
        "bankBalance" -> Attribute(FloatType()),
        "name"        -> Attribute(StringType()),
        "isStudent"   -> Attribute(BoolType),
        "dateOfBirth" -> Attribute(DateType),
        "timeOfDay"   -> Attribute(TimeType),
        "expiry"      -> Attribute(DateTimeType),
        "image"       -> Attribute(BlobType()),
        "fk"          -> Attribute(ForeignKey("OtherService")),
      ),
      datastore = Database.Postgres,
      readable = Readable.All,
      writable = Writable.All,
      projectUsesAuth = false,
      hasAuthBlock = false,
      metrics = None,
    )
  }

  it should "build a correct complex ServiceRoot with all endpoints" in {
    val serviceRoot: ServiceRoot = BuilderTestData.complexTemplefile.allServicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            endpoints = Set(Create, Read, Update, Delete, List),
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
        Update -> "UPDATE test_complex_service SET id = $1, another_id = $2, yet_another_id = $3, bank_balance = $4, big_bank_balance = $5, name = $6, initials = $7, is_student = $8, date_of_birth = $9, time_of_day = $10, expiry = $11, image = $12, sample_fk1 = $13, sample_fk2 = $14 WHERE id = $15 RETURNING id, another_id, yet_another_id, bank_balance, big_bank_balance, name, initials, is_student, date_of_birth, time_of_day, expiry, image, sample_fk1, sample_fk2;",
        Delete -> "DELETE FROM test_complex_service WHERE id = $1;",
      ),
      idAttribute = IDAttribute("id"),
      createdByAttribute = None,
      attributes = ListMap(
        "id"             -> Attribute(IntType(max = Some(100), min = Some(10), precision = 2)),
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
      AuthAttribute(ServiceAuth.Email, AttributeType.StringType()),
      IDAttribute("id"),
      "INSERT INTO auth (id, email, password) VALUES ($1, $2, $3) RETURNING id, email, password;",
      "SELECT id, email, password FROM auth WHERE email = $1;",
      metrics = None,
    )
  }

}
