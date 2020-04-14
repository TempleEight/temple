package temple.generate.server.go

import temple.ast.AttributeType
import temple.ast.Metadata.Metrics.Prometheus
import temple.ast.Metadata.ServiceAuth
import temple.generate.FileSystem._
import temple.generate.server.{AuthAttribute, AuthServiceRoot, IDAttribute}
import temple.utils.FileUtils._

object GoAuthServiceGeneratorTestData {

  val authServiceRoot: AuthServiceRoot = AuthServiceRoot(
    "github.com/TempleEight/spec-golang/auth",
    82,
    AuthAttribute(ServiceAuth.Email, AttributeType.StringType()),
    IDAttribute("id"),
    "INSERT INTO auth (id, email, password) VALUES ($1, $2, $3) RETURNING id, name, password",
    "SELECT id, email, password FROM auth WHERE email = $1",
    metrics = Some(Prometheus),
  )

  val authServiceFiles: Files = Map(
    File("auth", "go.mod")   -> readFile("src/test/scala/temple/generate/server/go/testfiles/auth/go.mod.snippet"),
    File("auth", "auth.go")  -> readFile("src/test/scala/temple/generate/server/go/testfiles/auth/auth.go.snippet"),
    File("auth", "setup.go") -> readFile("src/test/scala/temple/generate/server/go/testfiles/auth/setup.go.snippet"),
    File("auth", "hook.go")  -> readFile("src/test/scala/temple/generate/server/go/testfiles/auth/hook.go.snippet"),
    File("auth/dao", "errors.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/auth/dao/errors.go.snippet",
    ),
    File("auth/dao", "dao.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/auth/dao/dao.go.snippet",
    ),
    File("auth/dao", "datastore.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/auth/dao/datastore.go.snippet",
    ),
    File("auth/comm", "handler.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/auth/comm/handler.go.snippet",
    ),
    File("auth/util", "util.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/auth/util/util.go.snippet",
    ),
    File("auth/metric", "metric.go") -> readFile(
      "src/test/scala/temple/generate/server/go/testfiles/auth/metric/metric.go.snippet",
    ),
  )
}
