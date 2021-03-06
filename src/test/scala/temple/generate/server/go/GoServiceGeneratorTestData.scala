package temple.generate.server.go

import java.nio.file.Paths

import temple.ast.AbstractAttribute.Attribute
import temple.ast.Metadata.Database.Postgres
import temple.ast.Metadata.Metrics.Prometheus
import temple.ast.Metadata.{Readable, Writable}
import temple.ast.{Annotation, AttributeType}
import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.{CreatedByAttribute, IDAttribute, ServiceName}
import temple.utils.FileUtils

import scala.collection.immutable.{ListMap, SortedMap}

object GoServiceGeneratorTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    name = "User",
    module = "github.com/TempleEight/spec-golang/user",
    comms = Set.empty,
    opQueries = SortedMap(
      CRUD.Create -> "INSERT INTO user_temple (id, name) VALUES ($1, $2) RETURNING id, name",
      CRUD.Read   -> "SELECT id, name FROM user_temple WHERE id = $1",
      CRUD.Update -> "UPDATE user_temple SET name = $1 WHERE id = $2 RETURNING id, name",
      CRUD.Delete -> "DELETE FROM user_temple WHERE id = $1",
    ),
    port = 80,
    idAttribute = IDAttribute("id"),
    createdByAttribute = None,
    attributes = ListMap("name" -> Attribute(AttributeType.StringType(Option(255L), Option(2)))),
    structs = Nil,
    datastore = Postgres,
    readable = Readable.All,
    writable = Writable.This,
    projectUsesAuth = true,
    hasAuthBlock = true,
    metrics = Some(Prometheus),
  )

  val simpleServiceFiles: Files =
    FileUtils.buildFileMap(Paths.get("src/test/resources/generate-server-test/"), "user")

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      name = "Match",
      module = "github.com/TempleEight/spec-golang/match",
      comms = Set("User").map(ServiceName(_)),
      opQueries = SortedMap(
        CRUD.List   -> "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE created_by = $1",
        CRUD.Create -> "INSERT INTO match (id, created_by, userOne, userTwo, matchedOn) VALUES ($1, $2, $3, $4, $5) RETURNING id, created_by, userOne, userTwo, matchedOn",
        CRUD.Read   -> "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE id = $1",
        CRUD.Update -> "UPDATE match SET userOne = $1, userTwo = $2, matchedOn = $3 WHERE id = $4 RETURNING id, created_by, userOne, userTwo, matchedOn",
        CRUD.Delete -> "DELETE FROM match WHERE id = $1",
      ),
      port = 81,
      idAttribute = IDAttribute("id"),
      createdByAttribute = Some(CreatedByAttribute("authID", "createdBy")),
      attributes = ListMap(
        "userOne"   -> Attribute(AttributeType.ForeignKey("User")),
        "userTwo"   -> Attribute(AttributeType.ForeignKey("User")),
        "matchedOn" -> Attribute(AttributeType.DateTimeType, Some(Annotation.ServerSet)),
      ),
      structs = Nil,
      datastore = Postgres,
      readable = Readable.This,
      writable = Writable.This,
      projectUsesAuth = true,
      hasAuthBlock = false,
      metrics = Some(Prometheus),
    )

  val simpleServiceFilesWithComms: Files =
    FileUtils.buildFileMap(Paths.get("src/test/resources/generate-server-test/"), "match")
}
