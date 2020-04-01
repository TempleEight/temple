package temple.generate.server.go.auth

import temple.generate.server.AuthServiceRoot
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceMainGenerator {

  private[auth] def generateImports(root: AuthServiceRoot): String = {
    val standardImports = Seq("encoding/json", "flag", "fmt", "log", "net/http", "time").map(doubleQuote)

    val officialImports = doubleQuote("golang.org/x/crypto/bcrypt")

    val customImports = Seq(
      doubleQuote(s"${root.module}/comm"),
      doubleQuote(s"${root.module}/dao"),
      doubleQuote(s"${root.module}/util"),
      s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
      doubleQuote("github.com/dgrijalva/jwt-go"),
      doubleQuote("github.com/google/uuid"),
      doubleQuote("github.com/gorilla/mux"),
    )

    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", officialImports, "", customImports))
  }

  private[auth] def generateStructs(): String =
    FileUtils.readResources("go/genFiles/auth/main/structs.go.snippet").stripLineEnd

  private[auth] def generateRouter(): String =
    FileUtils.readResources("go/genFiles/auth/main/router.go.snippet").stripLineEnd

  private[auth] def generateHandlers(): String =
    FileUtils.readResources("go/genFiles/auth/main/handlers.go.snippet").stripLineEnd

  private[auth] def generateCreateToken(): String =
    FileUtils.readResources("go/genFiles/auth/main/create_token.go.snippet").stripLineEnd
}
