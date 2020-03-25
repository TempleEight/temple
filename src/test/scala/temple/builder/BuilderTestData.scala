package temple.builder

import temple.ast.AttributeType._
import temple.ast.Metadata.ServiceLanguage
import temple.ast._

import scala.collection.immutable.ListMap

object BuilderTestData {

  // TODO: This test doesn't include a `ForeignKey` attribute, since it is not yet supported
  //  by the parser/semantic analysis. Once it is, please update these tests!
  val sampleService: ServiceBlock = ServiceBlock(
    ListMap(
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

  val simpleTemplefile: Templefile = Templefile(
    "test-project",
    ProjectBlock(Seq(ServiceLanguage.Go)),
    targets = Map(),
    services = Map("test-service" -> sampleService),
  )

  val sampleComplexService: ServiceBlock = ServiceBlock(
    ListMap(
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
    structs = ListMap(
      "Test" -> StructBlock(
        ListMap(
          "favouriteColour" -> Attribute(StringType(), valueAnnotations = Set(Annotation.Unique)),
          "bedTime"         -> Attribute(TimeType, valueAnnotations = Set(Annotation.Nullable)),
          "favouriteNumber" -> Attribute(IntType(max = Some(10), min = Some(0))),
        ),
      ),
    ),
  )
}
