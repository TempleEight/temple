package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.AttributeType._
import temple.ast.Metadata.Database
import temple.ast.{Attribute, AttributeType}
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.CRUD._
import temple.generate.server.{CreatedByAttribute, IDAttribute, ServiceRoot}

import scala.collection.immutable.ListMap

class ServerBuilderTest extends FlatSpec with Matchers {

  behavior of "ServerBuilderTest"

  it should "build a correct simple ServiceRoot with all endpoints" in {
    val serviceRoot: ServiceRoot = BuilderTestData.simpleTemplefile.servicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            endpoints = Set(Create, Read, Update, Delete, List),
            port = port.service,
            detail = GoLanguageDetail("github.com/squat/and/dab"),
          )
    }
    serviceRoot shouldBe ServiceRoot(
      "test-service",
      "github.com/squat/and/dab/test-service",
      comms = Seq(),
      port = 1024,
      opQueries = ListMap(
        Create -> "INSERT INTO test-service (id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry, image) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry, image;",
        Read   -> "SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry, image FROM test-service WHERE id = $1;",
        Update -> "UPDATE test-service SET id = $1, bankBalance = $2, name = $3, isStudent = $4, dateOfBirth = $5, timeOfDay = $6, expiry = $7, image = $8 WHERE id = $9 RETURNING id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry, image;",
        Delete -> "DELETE FROM test-service WHERE id = $1;",
        List   -> "SELECT id, bankBalance, name, isStudent, dateOfBirth, timeOfDay, expiry, image FROM test-service;",
      ),
      idAttribute = IDAttribute("id"),
      createdByAttribute = CreatedByAttribute.None,
      attributes = ListMap(
        "id"          -> Attribute(IntType()),
        "bankBalance" -> Attribute(FloatType()),
        "name"        -> Attribute(StringType()),
        "isStudent"   -> Attribute(BoolType),
        "dateOfBirth" -> Attribute(DateType),
        "timeOfDay"   -> Attribute(TimeType),
        "expiry"      -> Attribute(DateTimeType),
        "image"       -> Attribute(BlobType()),
      ),
      datastore = Database.Postgres,
    )
  }

  it should "build a correct simple ServiceRoot with no endpoints" in {
    val serviceRoot: ServiceRoot = BuilderTestData.simpleTemplefile.servicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            endpoints = Set(),
            port = port.service,
            detail = GoLanguageDetail("github.com/squat/and/dab"),
          )
    }
    serviceRoot shouldBe ServiceRoot(
      "test-service",
      "github.com/squat/and/dab/test-service",
      comms = Seq(),
      port = 1024,
      opQueries = ListMap(),
      idAttribute = IDAttribute("id"),
      createdByAttribute = CreatedByAttribute.None,
      attributes = ListMap(
        "id"          -> Attribute(IntType()),
        "bankBalance" -> Attribute(FloatType()),
        "name"        -> Attribute(StringType()),
        "isStudent"   -> Attribute(BoolType),
        "dateOfBirth" -> Attribute(DateType),
        "timeOfDay"   -> Attribute(TimeType),
        "expiry"      -> Attribute(DateTimeType),
        "image"       -> Attribute(BlobType()),
      ),
      datastore = Database.Postgres,
    )
  }

  it should "build a correct complex ServiceRoot with all endpoints" in {
    val serviceRoot: ServiceRoot = BuilderTestData.complexTemplefile.servicesWithPorts.head match {
      case (name, service, port) =>
        ServerBuilder
          .buildServiceRoot(
            name,
            service,
            endpoints = Set(Create, Read, Update, Delete, List),
            port = port.service,
            detail = GoLanguageDetail("github.com/squat/and/dab"),
          )
    }
    serviceRoot shouldBe ServiceRoot(
      "test-complex-service",
      "github.com/squat/and/dab/test-complex-service",
      comms = Seq(),
      port = 1024,
      opQueries = ListMap(
        Create -> "INSERT INTO test-complex-service (id, anotherId, yetAnotherId, bankBalance, bigBankBalance, name, initials, isStudent, dateOfBirth, timeOfDay, expiry, image) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12) RETURNING id, anotherId, yetAnotherId, bankBalance, bigBankBalance, name, initials, isStudent, dateOfBirth, timeOfDay, expiry, image;",
        Read   -> "SELECT id, anotherId, yetAnotherId, bankBalance, bigBankBalance, name, initials, isStudent, dateOfBirth, timeOfDay, expiry, image FROM test-complex-service WHERE id = $1;",
        Update -> "UPDATE test-complex-service SET id = $1, anotherId = $2, yetAnotherId = $3, bankBalance = $4, bigBankBalance = $5, name = $6, initials = $7, isStudent = $8, dateOfBirth = $9, timeOfDay = $10, expiry = $11, image = $12 WHERE id = $13 RETURNING id, anotherId, yetAnotherId, bankBalance, bigBankBalance, name, initials, isStudent, dateOfBirth, timeOfDay, expiry, image;",
        Delete -> "DELETE FROM test-complex-service WHERE id = $1;",
        List   -> "SELECT id, anotherId, yetAnotherId, bankBalance, bigBankBalance, name, initials, isStudent, dateOfBirth, timeOfDay, expiry, image FROM test-complex-service;",
      ),
      idAttribute = IDAttribute("id"),
      createdByAttribute = CreatedByAttribute.None,
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
      ),
      datastore = Database.Postgres,
    )
  }

}
