package temple.generate.service.go

import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.service.ServiceRoot
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
    File("user", "go.mod")  -> readFile("src/test/scala/temple/generate/service/go/testfiles/user/go.mod"),
    File("user", "user.go") -> readFile("src/test/scala/temple/generate/service/go/testfiles/user/user.go"),
    File("user/dao", "errors.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/user/dao/errors.go",
    ),
    File("user/dao", "dao.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/user/dao/dao.go",
    ),
    File("user/util", "config.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/user/util/config.go",
    ),
    File("user/util", "util.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/user/util/util.go",
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
    File("match", "go.mod") -> readFile("src/test/scala/temple/generate/service/go/testfiles/match/go.mod"),
    File("match", "match.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/match/match.go",
    ),
    File("match/dao", "errors.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/match/dao/errors.go",
    ),
    File("match/dao", "dao.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/match/dao/dao.go",
    ),
    File("match/util", "config.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/match/util/config.go",
    ),
    File("match/util", "util.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/match/util/util.go",
    ),
    File("match/comm", "handler.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/match/comm/handler.go",
    ),
  )
}
