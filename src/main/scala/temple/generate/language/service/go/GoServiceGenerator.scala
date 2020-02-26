package temple.generate.language.service.go

import temple.generate.language.service.ServiceGenerator
import temple.generate.language.service.adt._
import temple.generate.utils.CodeTerm
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  private def generatePackage(packageName: String): String = s"package $packageName"

  /** Given a service name, module name and whether the service uses inter-service communication, return the import
    * block */
  private def generateImports(serviceName: String, module: String, usesComms: Boolean): String = {
    val standardImports = Seq("encoding/json", "flag", "fmt", "log", "net/http").map(doubleQuote)

    val customImports = Seq[CodeTerm](
      s"${serviceName}DAO ${doubleQuote(s"$module/dao")}",
      when(usesComms) { s"${serviceName}Comm ${doubleQuote(s"$module/comm")}" },
      doubleQuote(s"$module/util"),
      s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
      doubleQuote("github.com/gorilla/mux"),
    )

    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", customImports))
  }

  /** Given a service name and whether the service uses inter-service communication, return global statements */
  private def generateGlobals(serviceName: String, usesComms: Boolean): String =
    mkCode.lines(
      s"var dao ${serviceName}DAO.DAO",
      when(usesComms) { s"var comm ${serviceName}Comm.Handler" },
    )

  /** Given a service name, whether the service uses inter-service communication, the endpoints desired and the port
    * number, generate the main function */
  private def generateMain(serviceName: String, usesComms: Boolean, endpoints: Set[Endpoint], port: Int): String = {
    val sb = new StringBuilder
    sb.append("func main() ")

    val body = mkCode.lines(
      s"""configPtr := flag.String("config", "/etc/$serviceName-service/config.json", "configuration filepath")""",
      "flag.Parse()",
      "",
      "// Require all struct fields by default",
      "valid.SetFieldsRequiredByDefault(true)",
      "",
      "config, err := utils.GetConfig(*configPtr)",
      "if err != nil " + CodeWrap.curly.tabbed("log.Fatal(err)"),
      "",
      s"dao = ${serviceName}DAO.DAO{}",
      "err = dao.Init(config)",
      "if err != nil " + CodeWrap.curly.tabbed("log.Fatal(err)"),
      "",
      when[CodeTerm](usesComms) {
        Seq(
          s"comm = ${serviceName}Comm.Handler{}",
          "comm.Init(config)",
          "",
        )
      },
      "r := mux.NewRouter()",
      when[CodeTerm](endpoints.contains(Endpoint.ReadAll)) {
        Seq(
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

  private def generateJsonMiddleware(): String =
    FileUtils.readFile("src/main/scala/temple/generate/language/service/go/genFiles/JsonMiddleware.go").stripLineEnd

  private def generateHandler(serviceName: String, endpoint: Endpoint): String =
    s"func $serviceName${endpoint.verb}Handler(w http.ResponseWriter, r *http.Request) {}"

  private def generateHandlers(serviceName: String, endpoints: Set[Endpoint]): String =
    mkCode.doubleLines(
      for (endpoint <- Endpoint.values if endpoints.contains(endpoint))
        yield generateHandler(serviceName, endpoint),
    )

  private def generateErrors(serviceName: String): String =
    mkCode.lines(
      "package dao",
      "",
      """import "fmt"""",
      "",
      s"// Err${serviceName.capitalize}NotFound is returned when a $serviceName for the provided ID was not found",
      s"type Err${serviceName.capitalize}NotFound int64",
      "",
      s"func (e Err${serviceName.capitalize}NotFound) Error() string ${CodeWrap.curly
        .tabbed(s"""return fmt.Sprintf("$serviceName not found with ID %d", e)""")}",
    )

  override def generate(serviceRoot: ServiceRoot): Map[FileUtils.File, FileUtils.FileContent] = {
    val usesComms = serviceRoot.comms.nonEmpty
    val serviceString = mkCode.doubleLines(
      generatePackage("main"),
      generateImports(
        serviceRoot.name,
        serviceRoot.module,
        usesComms,
      ),
      generateGlobals(
        serviceRoot.name,
        usesComms,
      ),
      generateMain(
        serviceRoot.name,
        usesComms,
        serviceRoot.endpoints,
        serviceRoot.port,
      ),
      generateJsonMiddleware(),
      generateHandlers(
        serviceRoot.name,
        serviceRoot.endpoints,
      ),
    )
    val errorsString = generateErrors(serviceRoot.name)
    Map(
      FileUtils.File(serviceRoot.name, s"${serviceRoot.name}.go") -> (serviceString + "\n"),
      FileUtils.File(s"${serviceRoot.name}/dao", "errors.go")     -> (errorsString + "\n"),
    )
    /*
   * TODO:
   * Handler generation in <>.go
   * dao/<>-dao.go
   * dao/errors.go
   * utils/utils.go
   * utils/config.go
   * go.mod
   * go.sum
   * config.json
   */
  }
}
