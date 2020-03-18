package temple.generate.server.go

import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.server.ServiceRoot
import temple.utils.FileUtils._

object GoServiceGeneratorTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Set(CRUD.Create, CRUD.Read, CRUD.Update, CRUD.Delete),
    80,
  )

  val simpleServiceFiles: Map[File, FileContent] = Map(
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
    )

  val simpleServiceFilesWithComms: Map[File, FileContent] = Map(
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
