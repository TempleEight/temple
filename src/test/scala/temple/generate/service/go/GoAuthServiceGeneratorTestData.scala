package temple.generate.service.go.auth

import temple.generate.service.AuthServiceRoot
import temple.generate.FileSystem._
import temple.utils.FileUtils._

object GoAuthServiceGeneratorTestData {

  val authServiceRoot: AuthServiceRoot = AuthServiceRoot(
    "github.com/TempleEight/spec-golang/auth",
    82,
  )

  val authServiceFiles: Map[File, FileContent] = Map(
    File("auth", "go.mod")  -> readFile("src/test/scala/temple/generate/service/go/testfiles/auth/go.mod.snippet"),
    File("auth", "auth.go") -> readFile("src/test/scala/temple/generate/service/go/testfiles/auth/auth.go.snippet"),
    File("auth/comm", "handler.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/auth/comm/handler.go.snippet",
    ),
    File("auth/util", "util.go") -> readFile(
      "src/test/scala/temple/generate/service/go/testfiles/auth/util/util.go.snippet",
    ),
  )
}
