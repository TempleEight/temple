package temple.generate.server

import temple.generate.CRUD
import temple.ast.Attribute
import temple.ast.AttributeType
import temple.ast.Annotation

object GoGeneratorIntegrationTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Set(CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
    80,
    Map("id" -> Attribute(AttributeType.UUIDType), "name" -> Attribute(AttributeType.StringType())),
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Set(CRUD.ReadAll, CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
      81,
      Map(
        "id"         -> Attribute(AttributeType.UUIDType),
        "created_by" -> Attribute(AttributeType.StringType()),
        "userOne"    -> Attribute(AttributeType.UUIDType),
        "userTwo"    -> Attribute(AttributeType.UUIDType),
        "matchedOn"  -> Attribute(AttributeType.DateTimeType, Option(Annotation.ServerSet)),
      ),
    )
}
