package temple.builder

import temple.ast
import temple.ast.AttributeType._
import temple.ast._

import scala.collection.immutable.ListMap

object BuilderTestData {

  // TODO: This test doesn't include a `ForeignKey` attribute, since it is not yet supported
  //  by the parser/semantic analysis. Once it is, please update these tests!
  val sampleService: ServiceBlock = ServiceBlock(
    ListMap(
      "id"          -> Attribute(IntType()),
      "bankBalance" -> ast.Attribute(FloatType()),
      "name"        -> ast.Attribute(StringType()),
      "isStudent"   -> ast.Attribute(BoolType),
      "dateOfBirth" -> ast.Attribute(DateType),
      "timeOfDay"   -> ast.Attribute(TimeType),
      "expiry"      -> ast.Attribute(DateTimeType),
      "image"       -> ast.Attribute(BlobType()),
    ),
  )

  val sampleComplexService: ServiceBlock = ServiceBlock(
    ListMap(
      "id"             -> ast.Attribute(IntType(max = Some(100), min = Some(10), precision = 2)),
      "anotherId"      -> ast.Attribute(IntType(max = Some(100), min = Some(10))),
      "yetAnotherId"   -> ast.Attribute(IntType(max = Some(100), min = Some(10), precision = 8)),
      "bankBalance"    -> ast.Attribute(FloatType(max = Some(300), min = Some(0), precision = 4)),
      "bigBankBalance" -> ast.Attribute(FloatType(max = Some(123), min = Some(0))),
      "name"           -> ast.Attribute(StringType(max = None, min = Some(1))),
      "initials"       -> ast.Attribute(StringType(max = Some(5), min = Some(0))),
      "isStudent"      -> ast.Attribute(BoolType),
      "dateOfBirth"    -> ast.Attribute(DateType),
      "timeOfDay"      -> ast.Attribute(TimeType),
      "expiry"         -> ast.Attribute(DateTimeType),
      "image"          -> ast.Attribute(BlobType()),
    ),
    structs = ListMap(
      "Test" -> StructBlock(
        ListMap(
          "favouriteColour" -> ast.Attribute(StringType(), valueAnnotations = Set(Annotation.Unique)),
          "bedTime"         -> ast.Attribute(TimeType, valueAnnotations = Set(Annotation.Nullable)),
          "favouriteNumber" -> ast.Attribute(IntType(max = Some(10), min = Some(0))),
        ),
      ),
    ),
  )
}
