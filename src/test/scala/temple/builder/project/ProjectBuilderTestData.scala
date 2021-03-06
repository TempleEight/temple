package temple.builder.project

import temple.ast.AbstractAttribute.{Attribute, IDAttribute, ParentAttribute}
import temple.ast.AbstractServiceBlock._
import temple.ast.AttributeType._
import temple.ast.Metadata.{AuthMethod, Database, ServiceAuth}
import temple.ast._

import scala.collection.immutable.ListMap

object ProjectBuilderTestData {

  private val simpleServiceAttributes = ListMap(
    "id"            -> IDAttribute,
    "intField"      -> Attribute(IntType()),
    "doubleField"   -> Attribute(FloatType()),
    "stringField"   -> Attribute(StringType()),
    "boolField"     -> Attribute(BoolType),
    "dateField"     -> Attribute(DateType),
    "timeField"     -> Attribute(TimeType),
    "dateTimeField" -> Attribute(DateTimeType),
    "blobField"     -> Attribute(BlobType()),
  )

  private val simpleStructAttributes = ListMap(
    "id"            -> IDAttribute,
    "parentID"      -> ParentAttribute,
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
    "id"                 -> IDAttribute,
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
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresProject: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Seq(Database.Postgres)),
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresService: Templefile = Templefile(
    "SampleProject",
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes, metadata = Seq(Database.Postgres)),
    ),
  )

  val simpleTemplefileForeignKeyService: Templefile = Templefile(
    "SampleProject",
    services = Map(
      "A" -> ServiceBlock(
        attributes = Map("myB" -> Attribute(ForeignKey("B"))),
        structs = Map(
          "InnerStruct" -> StructBlock(
            attributes = Map("myC" -> Attribute(ForeignKey("C"))),
          ),
        ),
      ),
      "B" -> ServiceBlock(
        attributes = Map("example" -> Attribute(IntType())),
      ),
      "C" -> ServiceBlock(
        attributes = Map("anotherExample" -> Attribute(IntType())),
      ),
    ),
  )

  val complexTemplefile: Templefile = Templefile(
    "SampleComplexProject",
    projectBlock = ProjectBlock(
      metadata = Seq(
        Metadata.Metrics.Prometheus,
        Metadata.Provider.Kubernetes,
        AuthMethod.Email,
      ),
    ),
    services = Map(
      "ComplexUser" -> ServiceBlock(
        complexServiceAttributes,
        metadata = Seq(ServiceAuth),
        structs = Map("TempleUser" -> StructBlock(simpleStructAttributes)),
      ),
    ),
  )

  val foreignKeyConfigJson: String =
    """|{
       |  "user" : "postgres",
       |  "dbName" : "postgres",
       |  "host" : "a-db",
       |  "sslMode" : "disable",
       |  "services" : {
       |    "b" : "http://b:1028/b",
       |    "c" : "http://c:1030/c"
       |  },
       |  "ports" : {
       |    "service" : 1026
       |  }
       |}
       |""".stripMargin
}
