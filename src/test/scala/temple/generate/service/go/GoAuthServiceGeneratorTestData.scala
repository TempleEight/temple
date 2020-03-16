package temple.generate.service.go

import temple.generate.service.AuthServiceRoot
import temple.generate.FileSystem._
import temple.utils.FileUtils._

object GoAuthServiceGeneratorTestData {

  val authServiceRoot: AuthServiceRoot = AuthServiceRoot(
    "github.com/TempleEight/spec-golang/auth",
    82,
  )

  val authServiceFiles: Map[File, FileContent] = Map(
    File("auth", "go.mod")  -> readFile("src/test/scala/temple/generate/service/go/testfiles/auth/go.mod"),
    File("auth", "auth.go") -> readFile("src/test/scala/temple/generate/service/go/testfiles/auth/auth.go"),
  )
}