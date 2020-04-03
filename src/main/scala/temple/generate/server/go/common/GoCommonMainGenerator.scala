package temple.generate.server.go.common

import temple.generate.server.ServiceRoot
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.server.go.common.GoCommonGenerator._
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoCommonMainGenerator {

  private[go] def generateJsonMiddleware(): String =
    mkCode(
      "func jsonMiddleware(next http.Handler) http.Handler",
      CodeWrap.curly.tabbed(
        genReturn(
          s"http.${genFunctionCall(
            "HandlerFunc",
            mkCode(
              genFunctionCall("func", "w http.ResponseWriter", "r *http.Request"),
              CodeWrap.curly.tabbed(
                "// All responses are JSON, set header accordingly",
                s"w.Header().Set(${doubleQuote("Content-Type")}, ${doubleQuote("application/json")})",
                "next.ServeHTTP(w, r)",
              ),
            ),
          )}",
        ),
      ),
    )

  private[go] def generateMain(service: ServiceRoot.Name, port: Int, usesComms: Boolean, isAuth: Boolean): String =
    mkCode(
      "func main()",
      CodeWrap.curly.tabbed(
        genDeclareAndAssign(
          genMethodCall(
            "flag",
            "String",
            doubleQuote("config"),
            doubleQuote(s"/etc/${service.kebabName}-service/config.json"),
            doubleQuote("configuration filepath"),
          ),
          "configPtr",
        ),
        genMethodCall("flag", "Parse"),
        "",
        "// Require all struct fields by default",
        genMethodCall("valid", "SetFieldsRequiredByDefault", "true"),
        "",
        genDeclareAndAssign(genMethodCall("util", "GetConfig", "*configPtr"), "config", "err"),
        genIfErr(genMethodCall("log", "Fatal", "err")),
        "",
        genDeclareAndAssign(genMethodCall("dao", "Init", "config"), "d", "err"),
        genIfErr(genMethodCall("log", "Fatal", "err")),
        "",
        when(usesComms) {
          mkCode.lines(
            genDeclareAndAssign(genMethodCall("comm", "Init", "config"), "c"),
            "",
          )
        },
        when(isAuth) {
          mkCode.lines(
            genDeclareAndAssign(genMethodCall("c", "CreateJWTCredential"), "jwtCredential", "err"),
            genIfErr(genMethodCall("log", "Fatal", "err")),
            "",
          )
        },
        genDeclareAndAssign(
          s"env{${mkCode.list("d", when(usesComms) { "c" }, when(isAuth) { "jwtCredential" })}}",
          "env",
        ),
        "",
        genMethodCall(
          "log",
          "Fatal",
          genMethodCall("http", "ListenAndServe", doubleQuote(s":$port"), genMethodCall("env", "router")),
        ),
      ),
    )
}
