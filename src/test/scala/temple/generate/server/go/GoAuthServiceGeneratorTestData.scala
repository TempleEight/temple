package temple.generate.server.go

import java.nio.file.Paths

import temple.ast.AttributeType
import temple.ast.Metadata.AuthMethod
import temple.ast.Metadata.Metrics.Prometheus
import temple.generate.FileSystem.Files
import temple.generate.server.{AuthAttribute, AuthServiceRoot, IDAttribute}
import temple.utils.FileUtils

object GoAuthServiceGeneratorTestData {

  val authServiceRoot: AuthServiceRoot = AuthServiceRoot(
    "github.com/TempleEight/spec-golang/auth",
    82,
    AuthAttribute(AuthMethod.Email, AttributeType.StringType()),
    IDAttribute("id"),
    "INSERT INTO auth (id, email, password) VALUES ($1, $2, $3) RETURNING id, name, password",
    "SELECT id, email, password FROM auth WHERE email = $1",
    metrics = Some(Prometheus),
  )

  val authServiceFiles: Files =
    FileUtils.buildFileMap(Paths.get("src/test/resources/generate-server-test/"), "auth")
}
