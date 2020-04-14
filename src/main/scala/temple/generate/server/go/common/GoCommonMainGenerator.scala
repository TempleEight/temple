package temple.generate.server.go.common

import temple.generate.server.{ServiceName, ServiceRoot}
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
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

  private def generateFlagParseBlock(serviceName: ServiceName): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "flag",
          "String",
          doubleQuote("config"),
          doubleQuote(s"/etc/${serviceName.kebabName}-service/config.json"),
          doubleQuote("configuration filepath"),
        ),
        "configPtr",
      ),
      genMethodCall("flag", "Parse"),
    )

  private def generateRequireFieldsBlock(): String =
    mkCode.lines(
      "// Require all struct fields by default",
      genMethodCall("valid", "SetFieldsRequiredByDefault", "true"),
    )

  private def generateGetConfigBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("util", "GetConfig", "*configPtr"), "config", "err"),
      genIfErr(genMethodCall("log", "Fatal", "err")),
    )

  private def generateMetricsBlock(): String = {
    val goroutineBody = mkCode.lines(
      genMethodCall("http", "Handle", doubleQuote("/metrics"), genMethodCall("promhttp", "Handler")),
      genMethodCall("http", "ListenAndServe", genMethodCall("fmt", "Sprintf", doubleQuote(":%d"), "promPort"), "nil"),
    )
    mkCode.lines(
      "// Prometheus metrics",
      genDeclareAndAssign(s"config.Ports[${doubleQuote("prometheus")}]", "promPort", "ok"),
      genIf("!ok", genMethodCall("log", "Fatal", doubleQuote("A port for the key prometheus was not found"))),
      genAnonGoroutine(Seq.empty, goroutineBody, Seq.empty),
    )
  }

  private def generateDAOInitBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("dao", "Init", "config"), "d", "err"),
      genIfErr(genMethodCall("log", "Fatal", "err")),
    )

  private def generateCommsInitBlock(): String =
    genDeclareAndAssign(genMethodCall("comm", "Init", "config"), "c")

  private def generateJWTCredentialsBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("c", "CreateJWTCredential"), "jwtCredential", "err"),
      genIfErr(genMethodCall("log", "Fatal", "err")),
    )

  private def generateEnvDeclarationBlock(usesComms: Boolean, isAuth: Boolean): String =
    genDeclareAndAssign(
      s"env{${mkCode.list("d", "Hook{}", when(usesComms) { "c" }, when(isAuth) { "jwtCredential" })}}",
      "env",
    )

  private def generateSetupBlock(): String =
    mkCode.lines(
      "// Call into non-generated entry-point",
      genDeclareAndAssign(genFunctionCall("defaultRouter", "&env"), "router"),
      genMethodCall("env", "setup", "router"),
    )

  private def generateListenAndServeBlock(port: Int): String =
    genMethodCall(
      "log",
      "Fatal",
      genMethodCall("http", "ListenAndServe", doubleQuote(s":$port"), "router"),
    )

  private[go] def generateMain(
    serviceName: ServiceName,
    port: Int,
    usesComms: Boolean,
    isAuth: Boolean,
    usesMetrics: Boolean,
  ): String =
    mkCode(
      "func main()",
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          generateFlagParseBlock(serviceName),
          generateRequireFieldsBlock(),
          generateGetConfigBlock(),
          when(usesMetrics) { generateMetricsBlock() },
          generateDAOInitBlock(),
          when(usesComms) { generateCommsInitBlock() },
          when(isAuth) { generateJWTCredentialsBlock() },
          generateEnvDeclarationBlock(usesComms, isAuth),
          generateSetupBlock(),
          generateListenAndServeBlock(port),
        ),
      ),
    )
}
