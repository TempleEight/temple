package temple.generate.service.go

import temple.generate.Endpoint
import temple.generate.service.ServiceGenerator
import temple.generate.utils.CodeTerm
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoServiceMainGenerator {

  /** Given a service name, module name and whether the service uses inter-service communication, return the import
    * block */
  private[go] def generateImports(serviceName: String, module: String, usesComms: Boolean): String = {
    val standardImports = Seq("flag", "log", "net/http").map(doubleQuote)

    val customImports = Seq[CodeTerm](
      when(usesComms) { s"${serviceName}Comm ${doubleQuote(s"$module/comm")}" },
      s"${serviceName}DAO ${doubleQuote(s"$module/dao")}",
      doubleQuote(s"$module/util"),
      s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
      doubleQuote("github.com/gorilla/mux"),
    )

    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", customImports))
  }

  /** Given a service name and whether the service uses inter-service communication, return global statements */
  private[go] def generateGlobals(serviceName: String, usesComms: Boolean): String =
    mkCode.lines(
      s"var dao ${serviceName}DAO.DAO",
      when(usesComms) { s"var comm ${serviceName}Comm.Handler" },
    )

  /** Given a service name, whether the service uses inter-service communication, the endpoints desired and the port
    * number, generate the main function */
  private[go] def generateMain(serviceName: String, usesComms: Boolean, endpoints: Set[Endpoint], port: Int): String = {
    val sb = new StringBuilder
    sb.append("func main() ")

    val body = mkCode.lines(
      s"""configPtr := flag.String("config", "/etc/$serviceName-service/config.json", "configuration filepath")""",
      "flag.Parse()",
      "",
      "// Require all struct fields by default",
      "valid.SetFieldsRequiredByDefault(true)",
      "",
      "config, err := util.GetConfig(*configPtr)",
      "if err != nil " + CodeWrap.curly.tabbed("log.Fatal(err)"),
      "",
      s"dao = ${serviceName}DAO.DAO{}",
      "err = dao.Init(config)",
      "if err != nil " + CodeWrap.curly.tabbed("log.Fatal(err)"),
      "",
      when[CodeTerm](usesComms) {
        mkCode.lines(
          s"comm = ${serviceName}Comm.Handler{}",
          "comm.Init(config)",
          "",
        )
      },
      "r := mux.NewRouter()",
      when[CodeTerm](endpoints.contains(Endpoint.ReadAll)) {
        mkCode.lines(
          "// Mux directs to first matching route, i.e. the order matters",
          s"""r.HandleFunc("/$serviceName/all", ${serviceName}ListHandler).Methods(http.MethodGet)""",
        )
      },
      when(endpoints.contains(Endpoint.Create)) {
        s"""r.HandleFunc("/$serviceName", ${serviceName}CreateHandler).Methods(http.MethodPost)"""
      },
      when(endpoints.contains(Endpoint.Read)) {
        s"""r.HandleFunc("/$serviceName/{id}", ${serviceName}ReadHandler).Methods(http.MethodGet)"""
      },
      when(endpoints.contains(Endpoint.Update)) {
        s"""r.HandleFunc("/$serviceName/{id}", ${serviceName}UpdateHandler).Methods(http.MethodPut)"""
      },
      when(endpoints.contains(Endpoint.Delete)) {
        s"""r.HandleFunc("/$serviceName/{id}", ${serviceName}DeleteHandler).Methods(http.MethodDelete)"""
      },
      "r.Use(jsonMiddleware)",
      "",
      s"""log.Fatal(http.ListenAndServe(":$port", r))""",
    )

    mkCode("func main()", CodeWrap.curly.tabbed(body))
  }

  private[go] def generateJsonMiddleware(): String =
    FileUtils.readResources("go/genFiles/json_middleware.go").stripLineEnd

  private[go] def generateHandler(serviceName: String, endpoint: Endpoint): String =
    s"func $serviceName${ServiceGenerator.verb(endpoint)}Handler(w http.ResponseWriter, r *http.Request) {}"

  private[go] def generateHandlers(serviceName: String, endpoints: Set[Endpoint]): String =
    mkCode.doubleLines(
      for (endpoint <- Endpoint.values if endpoints.contains(endpoint))
        yield generateHandler(serviceName, endpoint),
    )
}
