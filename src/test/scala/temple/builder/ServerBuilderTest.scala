package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.AttributeType._
import temple.ast.{Attribute, AttributeType}
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
            endpoints = Set(Create, Read, Update, Delete),
            port = port,
          )
    }
    serviceRoot shouldBe ServiceRoot(
      "test-service",
      "test-service",
      comms = Seq(),
      port = 1024,
      operations = Set(Create, Read, Update, Delete),
      idAttribute = IDAttribute("id", AttributeType.UUIDType),
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
    )
  }

}
