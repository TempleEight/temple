package temple.builder.project

import temple.DSL.semantics.AttributeType._
import temple.DSL.semantics.Metadata.Database
import temple.DSL.semantics._

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
    ProjectBlock(),
    Map(),
    Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresProject: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Seq(Database.Postgres)),
    Map(),
    Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresService: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(),
    Map(),
    Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes, metadata = Seq(Database.Postgres)),
    ),
  )

  val simpleTemplefilePostgresCreateOutput: String =
    """CREATE TABLE temple_user (
      |  intField INT NOT NULL,
      |  doubleField DOUBLE PRECISION NOT NULL,
      |  stringField TEXT NOT NULL,
      |  boolField BOOLEAN NOT NULL,
      |  dateField DATE NOT NULL,
      |  timeField TIME NOT NULL,
      |  dateTimeField TIMESTAMPTZ NOT NULL,
      |  blobField BYTEA NOT NULL
      |);""".stripMargin

  val simpleTemplefileUsersDockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /templeuser
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/templeuser-service
      |
      |RUN ["go", "build", "-o", "templeuser"]
      |
      |ENTRYPOINT ["./templeuser"]
      |
      |EXPOSE 80
      |""".stripMargin

  val complexTemplefile: Templefile = Templefile(
    "SampleComplexProject",
    ProjectBlock(),
    Map(),
    Map(
      "ComplexUser" -> ServiceBlock(
        complexServiceAttributes,
        structs = Map("TempleUser" -> StructBlock(simpleServiceAttributes)),
      ),
    ),
  )

  val complexTemplefilePostgresCreateOutput: String =
    """CREATE TABLE complex_user (
      |  smallIntField SMALLINT CHECK (smallIntField <= 100) CHECK (smallIntField >= 10) NOT NULL,
      |  intField INT CHECK (intField <= 100) CHECK (intField >= 10) NOT NULL,
      |  bigIntField BIGINT CHECK (bigIntField <= 100) CHECK (bigIntField >= 10) NOT NULL,
      |  floatField REAL CHECK (floatField <= 300.0) CHECK (floatField >= 0.0) NOT NULL,
      |  doubleField DOUBLE PRECISION CHECK (doubleField <= 123.0) CHECK (doubleField >= 0.0) NOT NULL,
      |  stringField TEXT CHECK (length(stringField) >= 1) NOT NULL,
      |  boundedStringField VARCHAR(5) CHECK (length(boundedStringField) >= 0) NOT NULL,
      |  boolField BOOLEAN NOT NULL,
      |  dateField DATE NOT NULL,
      |  timeField TIME NOT NULL,
      |  dateTimeField TIMESTAMPTZ NOT NULL,
      |  blobField BYTEA NOT NULL
      |);""".stripMargin + "\n\n" + simpleTemplefilePostgresCreateOutput

  val complexTemplefileUsersDockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /complexuser
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/complexuser-service
      |
      |RUN ["go", "build", "-o", "complexuser"]
      |
      |ENTRYPOINT ["./complexuser"]
      |
      |EXPOSE 80
      |""".stripMargin
}
