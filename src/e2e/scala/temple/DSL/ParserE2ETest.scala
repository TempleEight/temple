package temple.DSL

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.parser.DSLParserMatchers
import temple.DSL.semantics.Analyzer.parseAndValidate
import temple.ast.AbstractServiceBlock._
import temple.ast.Annotation.{Nullable, Server, Unique}
import temple.ast.AttributeType._
import temple.ast.Metadata._
import temple.ast._
import temple.utils.FileUtils.readFile

import scala.collection.immutable.ListMap

class ParserE2ETest extends FlatSpec with Matchers with DSLParserMatchers {
  behavior of "Temple parser"

  it should "parse and analyze simple.temple correctly" in {
    val source      = readFile("src/test/scala/temple/testfiles/simple.temple")
    val parseResult = DSLProcessor.parse(source).shouldParse
    val semantics   = parseAndValidate(parseResult)
    semantics shouldBe Templefile(
      projectName = "SimpleTempleTest",
      projectBlock = ProjectBlock(
        metadata = Seq(
          Metrics.Prometheus,
        ),
      ),
      services = Map(
        "SimpleTempleTestUser" -> ServiceBlock(
          attributes = ListMap(
            "simpleTempleTestUser" -> Attribute(StringType()),
            "email"                -> Attribute(StringType(Some(40), Some(5))),
            "firstName"            -> Attribute(StringType()),
            "lastName"             -> Attribute(StringType()),
            "createdAt"            -> Attribute(DateTimeType),
            "numberOfDogs"         -> Attribute(IntType()),
            "yeets"                -> Attribute(BoolType, Some(Server), Set(Unique)),
            "currentBankBalance"   -> Attribute(FloatType(min = Some(0.0), precision = 2)),
            "birthDate"            -> Attribute(DateType),
            "breakfastTime"        -> Attribute(TimeType),
          ),
          metadata = Seq(
            ServiceEnumerable,
            Omit(Set(Endpoint.Delete)),
            Readable.All,
            Writable.This,
            ServiceAuth.Email,
            Uses(Seq("Booking", "SimpleTempleTestGroup")),
          ),
          structs = Map(
            "Fred" -> StructBlock(
              Map(
                "field"  -> Attribute(StringType(), valueAnnotations = Set(Nullable)),
                "friend" -> Attribute(ForeignKey("SimpleTempleTestUser")),
                "image"  -> Attribute(BlobType(size = Some(10_000_000))),
              ),
              Seq(ServiceEnumerable, Readable.This),
            ),
          ),
        ),
        "Booking"               -> ServiceBlock(Map()),
        "SimpleTempleTestGroup" -> ServiceBlock(Map()),
      ),
    )
  }
}
