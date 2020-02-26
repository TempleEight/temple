package temple.generate.language.service.go

import temple.generate.language.service.adt._
import temple.utils.FileUtils._

object GoServiceGeneratorTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Set(Endpoint.Create, Endpoint.Read, Endpoint.Update, Endpoint.Delete),
    80,
  )

  val simpleServiceFiles: Map[File, FileContent] = Map(
    File("user", "user.go") -> readFile("src/test/scala/temple/generate/language/service/go/testfiles/user/user.go"),
    File("user/dao", "errors.go") -> readFile(
      "src/test/scala/temple/generate/language/service/go/testfiles/user/dao/errors.go",
    ),
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Set(Endpoint.ReadAll, Endpoint.Create, Endpoint.Read, Endpoint.Update, Endpoint.Delete),
      81,
    )

  val simpleServiceFilesWithComms: Map[File, FileContent] = Map(
    File("match", "match.go") -> readFile(
      "src/test/scala/temple/generate/language/service/go/testfiles/match/match.go",
    ),
    File("match/dao", "errors.go") -> readFile(
      "src/test/scala/temple/generate/language/service/go/testfiles/match/dao/errors.go",
    ),
  )
}
