package temple.generate.server.go

import temple.ast.Metadata.Database.Postgres
import temple.ast.{Annotation, Attribute, AttributeType}
import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.server.{CreatedByAttribute, IDAttribute, ServiceRoot}
import temple.utils.FileUtils._

import scala.collection.immutable.ListMap

object GoServiceGeneratorTestData {

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
    IDAttribute("id"),
    CreatedByAttribute.None,
    ListMap("name" -> Attribute(AttributeType.StringType(Option(255L), Option(2)))),
    Postgres,
  )

  val simpleServiceFiles: Files = Map(
    File("user", "go.mod")  -> readFile("src/test/scala/temple/generate/server/go/testfiles/user/go.mod.snippet"),
    File("user", "user.go") -> readFile("src/test/scala/temple/generate/server/go/testfiles/user/user.go.snippet"),
    File("user/dao", "errors.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/user/dao/errors.go.snippet",
    ),
    File("user/dao", "dao.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/user/dao/dao.go.snippet",
    ),
    File("user/util", "util.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/user/util/util.go.snippet",
    ),
    File("user/metric", "metric.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/user/metric/metric.go.snippet",
    ),
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      ListMap(
        CRUD.List   -> "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE created_by = $1",
        CRUD.Create -> "INSERT INTO match (id, created_by, userOne, userTwo, matchedOn) VALUES ($1, $2, $3, $4, NOW()) RETURNING id, created_by, userOne, userTwo, matchedOn",
        CRUD.Read   -> "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE id = $1",
        CRUD.Update -> "UPDATE match SET userOne = $1, userTwo = $2, matchedOn = NOW() WHERE id = $3 RETURNING id, created_by, userOne, userTwo, matchedOn",
        CRUD.Delete -> "DELETE FROM match WHERE id = $1",
      ),
      81,
      IDAttribute("id"),
      CreatedByAttribute.EnumerateByCreator("authID", "createdBy"),
      ListMap(
        "userOne"   -> Attribute(AttributeType.UUIDType),
        "userTwo"   -> Attribute(AttributeType.UUIDType),
        "matchedOn" -> Attribute(AttributeType.DateTimeType, Some(Annotation.ServerSet)),
      ),
      Postgres,
    )

  val simpleServiceFilesWithComms: Files = Map(
    File("match", "go.mod") -> readFile("src/test/scala/temple/generate/server/go/testfiles/match/go.mod.snippet"),
    File("match", "match.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/match/match.go.snippet",
    ),
    File("match/dao", "errors.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/match/dao/errors.go.snippet",
    ),
    File("match/dao", "dao.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/match/dao/dao.go.snippet",
    ),
    File("match/util", "util.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/match/util/util.go.snippet",
    ),
    File("match/comm", "handler.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/match/comm/handler.go.snippet",
    ),
    File("match/metric", "metric.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/match/metric/metric.go.snippet",
    ),
  )
}
