package temple.builder.project

import temple.DSL.semantics.AttributeType.{BlobType, BoolType, DateTimeType, DateType, FloatType, IntType, StringType, TimeType}
import temple.DSL.semantics.Metadata.Database
import temple.DSL.semantics.{Attribute, ProjectBlock, ServiceBlock, StructBlock, Templefile}

import scala.collection.immutable.ListMap

object ProjectBuilderTestData {

  private val simpleServiceAttributes = ListMap(
    "intField"      -> Attribute(IntType()),
    "doubleField"   -> Attribute(FloatType()),
    "stringField"   -> Attribute(StringType()),
    "boolField"     -> Attribute(BoolType),
    "dateField"     -> Attribute(DateType),
    "timeField"     -> Attribute(TimeType),
    "dateTimeField" -> Attribute(DateTimeType),
    "blobField"     -> Attribute(BlobType()),
  )

  private val complexServiceAttributes = ListMap(
    "smallIntField"      -> Attribute(IntType(max = Some(100), min = Some(10), precision = 2)),
    "intField"           -> Attribute(IntType(max = Some(100), min = Some(10))),
    "bigIntField"        -> Attribute(IntType(max = Some(100), min = Some(10), precision = 8)),
    "floatField"         -> Attribute(FloatType(max = Some(300), min = Some(0), precision = 4)),
    "doubleField"        -> Attribute(FloatType(max = Some(123), min = Some(0))),
    "stringField"        -> Attribute(StringType(max = None, min = Some(1))),
    "boundedStringField" -> Attribute(StringType(max = Some(5), min = Some(0))),
    "boolField"          -> Attribute(BoolType),
    "dateField"          -> Attribute(DateType),
    "timeField"          -> Attribute(TimeType),
    "dateTimeField"      -> Attribute(DateTimeType),
    "blobField"          -> Attribute(BlobType()),
  )

  val simpleTemplefile: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Nil),
    Map(),
    Map(
      "Users" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresProject: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Seq(Database.Postgres)),
    Map(),
    Map(
      "Users" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresService: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Nil),
    Map(),
    Map(
      "Users" -> ServiceBlock(simpleServiceAttributes, metadata = Seq(Database.Postgres)),
    ),
  )

  val simpleTemplefilePostgresCreateOutput: String =
    """CREATE TABLE Users (
      |  intField INT,
      |  doubleField DOUBLE PRECISION,
      |  stringField TEXT,
      |  boolField BOOLEAN,
      |  dateField DATE,
      |  timeField TIME,
      |  dateTimeField TIMESTAMPTZ,
      |  blobField BYTEA
      |);""".stripMargin

  val complexTemplefile: Templefile = Templefile(
    "SampleComplexProject",
    ProjectBlock(Nil),
    Map(),
    Map(
      "ComplexUsers" -> ServiceBlock(
        complexServiceAttributes,
        structs = Map("Users" -> StructBlock(simpleServiceAttributes)),
      ),
    ),
  )

  val complexTemplefilePostgresCreateOutput: String =
    """CREATE TABLE ComplexUsers (
      |  smallIntField SMALLINT CHECK (smallIntField <= 100) CHECK (smallIntField >= 10),
      |  intField INT CHECK (intField <= 100) CHECK (intField >= 10),
      |  bigIntField BIGINT CHECK (bigIntField <= 100) CHECK (bigIntField >= 10),
      |  floatField REAL CHECK (floatField <= 300.0) CHECK (floatField >= 0.0),
      |  doubleField DOUBLE PRECISION CHECK (doubleField <= 123.0) CHECK (doubleField >= 0.0),
      |  stringField TEXT CHECK (length(stringField) >= 1),
      |  boundedStringField VARCHAR(5) CHECK (length(boundedStringField) >= 0),
      |  boolField BOOLEAN,
      |  dateField DATE,
      |  timeField TIME,
      |  dateTimeField TIMESTAMPTZ,
      |  blobField BYTEA
      |);""".stripMargin + "\n" + simpleTemplefilePostgresCreateOutput

}
