package temple.generate.server

import temple.ast.{Annotation, Attribute, AttributeType}
import temple.ast.Metadata.Database.Postgres
import temple.generate.CRUD

import scala.collection.immutable.ListMap

object GoGeneratorIntegrationTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    ListMap(
      CRUD.Create -> "INSERT INTO user_temple (id, name) VALUES ($1, $2) RETURNING id, name",
      CRUD.Read   -> "SELECT id, name FROM user_temple WHERE id = $1",
      CRUD.Update -> "UPDATE user_temple SET name = $1 WHERE id = $2 RETURNING id, name",
      CRUD.Delete -> "DELETE FROM user_temple WHERE id = $1",
    ),
    80,
    IDAttribute("id", AttributeType.UUIDType),
    CreatedByAttribute.None,
    ListMap("name" -> Attribute(AttributeType.StringType())),
    Postgres,
  )

  val simpleServiceRootWithComms: ServiceRoot = ServiceRoot(
    "match",
    "github.com/TempleEight/spec-golang/match",
    Seq("user"),
    ListMap(
      CRUD.ReadAll -> "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE created_by = $1",
      CRUD.Create  -> "INSERT INTO match (id, created_by, userOne, userTwo, matchedOn) VALUES ($1, $2, $3, $4, NOW()) RETURNING id, created_by, userOne, userTwo, matchedOn",
      CRUD.Read    -> "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE id = $1",
      CRUD.Update  -> "UPDATE match SET userOne = $1, userTwo = $2, matchedOn = NOW() WHERE id = $3 RETURNING id, created_by, userOne, userTwo, matchedOn",
      CRUD.Delete  -> "DELETE FROM match WHERE id = $1",
    ),
    81,
    IDAttribute("id", AttributeType.UUIDType),
    CreatedByAttribute.EnumerateByCreator("authID", "createdBy", AttributeType.UUIDType),
    ListMap(
      "userOne"   -> Attribute(AttributeType.UUIDType),
      "userTwo"   -> Attribute(AttributeType.UUIDType),
      "matchedOn" -> Attribute(AttributeType.DateTimeType, Some(Annotation.ServerSet)),
    ),
    Postgres,
  )
}
