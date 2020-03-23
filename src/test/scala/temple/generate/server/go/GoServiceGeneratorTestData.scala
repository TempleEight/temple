package temple.generate.server.go

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
    Set(CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
    80,
    IDAttribute("id", AttributeType.UUIDType),
    CreatedByAttribute.None,
    ListMap("name" -> Attribute(AttributeType.StringType())),
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
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Set(CRUD.ReadAll, CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
      81,
      IDAttribute("id", AttributeType.UUIDType),
      CreatedByAttribute.EnumerateByCreator("authID", "createdBy", AttributeType.UUIDType),
      ListMap(
        "userOne"   -> Attribute(AttributeType.UUIDType),
        "userTwo"   -> Attribute(AttributeType.UUIDType),
        "matchedOn" -> Attribute(AttributeType.DateTimeType, Some(Annotation.ServerSet)),
      ),
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
  )
}
