package temple.generate.server

import temple.ast.{Annotation, Attribute, AttributeType}
import temple.generate.CRUD

import scala.collection.immutable.ListMap

object GoGeneratorIntegrationTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Set(CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
    80,
    IDAttribute("id", AttributeType.UUIDType),
    CreatedByAttribute.None,
    ListMap("name" -> Attribute(AttributeType.StringType())),
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Set(CRUD.List, CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
      81,
      IDAttribute("id", AttributeType.UUIDType),
      CreatedByAttribute.EnumerateByCreator("authID", "createdBy", AttributeType.UUIDType),
      ListMap(
        "userOne"   -> Attribute(AttributeType.UUIDType),
        "userTwo"   -> Attribute(AttributeType.UUIDType),
        "matchedOn" -> Attribute(AttributeType.DateTimeType, Some(Annotation.ServerSet)),
      ),
    )
}
