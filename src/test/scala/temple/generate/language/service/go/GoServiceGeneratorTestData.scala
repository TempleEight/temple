package temple.generate.language.service.go

import temple.generate.language.service.adt._
import temple.utils.FileUtils._

object GoServiceGeneratorTestData {

  val simpleServiceRoot: ServiceRoot = ServiceRoot(
    "user",
    "github.com/TempleEight/spec-golang/user",
    Seq.empty,
    Seq(Endpoint.Create, Endpoint.Read, Endpoint.Update, Endpoint.Delete),
    80,
  )

  val simpleServiceFiles: Map[File, FileContent] = Map(
    File("user", "user.go") -> readFile("src/test/scala/temple/generate/language/service/go/testfiles/user.go"),
  )

  val simpleServiceRootWithComms: ServiceRoot =
    ServiceRoot(
      "match",
      "github.com/TempleEight/spec-golang/match",
      Seq("user"),
      Seq(Endpoint.All, Endpoint.Create, Endpoint.Read, Endpoint.Update, Endpoint.Delete),
      81,
    )

  val simpleServiceFilesWithComms: Map[File, FileContent] = Map(
    File("match", "match.go") -> readFile("src/test/scala/temple/generate/language/service/go/testfiles/match.go"),
  )
}
