package temple.builder

import temple.DSL.semantics.AttributeType._
import temple.DSL.semantics.{Annotation, Attribute, ServiceBlock, StructBlock}
import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint.{Check, Unique}
import temple.generate.database.ast.ComparisonOperator.{GreaterEqual, LessEqual}
import temple.generate.database.ast.Statement.Create
import temple.generate.database.ast.{ColumnDef, Statement}

import scala.collection.immutable.ListMap

object DatabaseBuilderTestData {

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

  val sampleServiceCreate: Seq[Statement.Create] =
    Seq(
      Create(
        "Users",
        Seq(
          ColumnDef("id", IntCol(4)),
          ColumnDef("bankBalance", FloatCol(8)),
          ColumnDef("name", StringCol),
          ColumnDef("isStudent", BoolCol),
          ColumnDef("dateOfBirth", DateCol),
          ColumnDef("timeOfDay", TimeCol),
          ColumnDef("expiry", DateTimeTzCol),
          ColumnDef("image", BlobCol),
        ),
      ),
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
          "bedTime"         -> Attribute(TimeType),
          "favouriteNumber" -> Attribute(IntType(max = Some(10), min = Some(0))),
        ),
      ),
    ),
  )

  val sampleComplexServiceCreate: Seq[Statement.Create] =
    Seq(
      Create(
        "Test",
        Seq(
          ColumnDef("favouriteColour", StringCol, Seq(Unique)),
          ColumnDef("bedTime", TimeCol),
          ColumnDef(
            "favouriteNumber",
            IntCol(4),
            Seq(
              Check("favouriteNumber", LessEqual, "10"),
              Check("favouriteNumber", GreaterEqual, "0"),
            ),
          ),
        ),
      ),
      Create(
        "Users",
        Seq(
          ColumnDef("id", IntCol(2), Seq(Check("id", LessEqual, "100"), Check("id", GreaterEqual, "10"))),
          ColumnDef(
            "anotherId",
            IntCol(4),
            Seq(Check("anotherId", LessEqual, "100"), Check("anotherId", GreaterEqual, "10")),
          ),
          ColumnDef(
            "yetAnotherId",
            IntCol(8),
            Seq(Check("yetAnotherId", LessEqual, "100"), Check("yetAnotherId", GreaterEqual, "10")),
          ),
          ColumnDef(
            "bankBalance",
            FloatCol(4),
            Seq(Check("bankBalance", LessEqual, "300.0"), Check("bankBalance", GreaterEqual, "0.0")),
          ),
          ColumnDef(
            "bigBankBalance",
            FloatCol(8),
            Seq(Check("bigBankBalance", LessEqual, "123.0"), Check("bigBankBalance", GreaterEqual, "0.0")),
          ),
          ColumnDef("name", StringCol, Seq(Check("length(name)", GreaterEqual, "1"))),
          ColumnDef("initials", BoundedStringCol(5), Seq(Check("length(initials)", GreaterEqual, "0"))),
          ColumnDef("isStudent", BoolCol),
          ColumnDef("dateOfBirth", DateCol),
          ColumnDef("timeOfDay", TimeCol),
          ColumnDef("expiry", DateTimeTzCol),
          ColumnDef("image", BlobCol),
        ),
      ),
    )
}
