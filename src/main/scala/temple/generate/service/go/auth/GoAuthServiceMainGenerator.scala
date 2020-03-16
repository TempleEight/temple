package temple.generate.service.go.auth

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceMainGenerator {

  private[auth] def generateImports(module: String): String = {
    val standardImports = Seq("encoding/json", "flag", "fmt", "log", "net/http", "time").map(doubleQuote)

    val officialImports = doubleQuote("golang.org/x/crypto/bcrypt")

    val customImports = Seq(
      doubleQuote(s"$module/comm"),
      doubleQuote(s"$module/dao"),
      doubleQuote(s"$module/util"),
      s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
      doubleQuote("github.com/dgrijalva/jwt-go"),
      doubleQuote("github.com/google/uuid"),
      doubleQuote("github.com/gorilla/mux"),
    )

    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", officialImports, "", customImports))
  }

  private[auth] def generateStructs(): String =
    FileUtils.readResources("go/genFiles/auth/auth_structs.go").stripLineEnd

  private[auth] def generateRouter(): String =
    FileUtils.readResources("go/genFiles/auth/auth_router.go").stripLineEnd

  private[auth] def generateMain(): String =
    mkCode(
      "func main() ",
      CodeWrap.curly.tabbed(
        mkCode.lines(
          "",
        ),
      ),
    )
}
