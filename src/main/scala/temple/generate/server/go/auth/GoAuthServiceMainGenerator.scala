package temple.generate.server.go.auth

import temple.generate.server.AuthServiceRoot
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoAuthServiceMainGenerator {

  private[auth] def generateImports(root: AuthServiceRoot, usesMetrics: Boolean): String =
    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        doubleQuote("encoding/json"),
        doubleQuote("flag"),
        doubleQuote("fmt"),
        doubleQuote("log"),
        doubleQuote("net/http"),
        when(usesMetrics) { doubleQuote("strconv") },
        doubleQuote("time"),
        "",
        doubleQuote("golang.org/x/crypto/bcrypt"),
        "",
        doubleQuote(s"${root.module}/comm"),
        doubleQuote(s"${root.module}/dao"),
        when(usesMetrics) { doubleQuote(s"${root.module}/metric") },
        doubleQuote(s"${root.module}/util"),
        s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
        doubleQuote("github.com/dgrijalva/jwt-go"),
        doubleQuote("github.com/google/uuid"),
        doubleQuote("github.com/gorilla/mux"),
        when(usesMetrics) {
          mkCode.lines(
            doubleQuote("github.com/prometheus/client_golang/prometheus"),
            doubleQuote("github.com/prometheus/client_golang/prometheus/promhttp"),
          )
        },
      ),
    )

  private[auth] def generateStructs(): String =
    FileUtils.readResources("go/genFiles/auth/main/structs.go.snippet").stripLineEnd

  private[auth] def generateRouter(): String =
    FileUtils.readResources("go/genFiles/auth/main/router.go.snippet").stripLineEnd

  private[auth] def generateHandlers(usesMetric: Boolean): String =
    if (usesMetric) FileUtils.readResources("go/genFiles/auth/main/handlers-metric.go.snippet").stripLineEnd
    else FileUtils.readResources("go/genFiles/auth/main/handlers-no-metric.go.snippet").stripLineEnd

  private[auth] def generateCreateToken(): String =
    FileUtils.readResources("go/genFiles/auth/main/create_token.go.snippet").stripLineEnd
}
