package temple.generate.language.service.go

import temple.generate.language.service.ServiceGenerator
import temple.generate.language.service.adt._
import temple.generate.utils.CodeTerm.CodeWrap
import temple.utils.FileUtils
import temple.utils.StringUtils.{doubleQuote, tabIndent}
import scala.collection.mutable.ListBuffer

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  private def generatePackage(packageName: String): String = s"package $packageName\n"

  /** Given a service name, module name and whether the service uses inter-service communication, return the import
    * block */
  private def generateImports(serviceName: String, module: String, usesComms: Boolean): String = {
    val sb = new StringBuilder
    sb.append("import ")

    val standardImports = Seq("encoding/json", "flag", "fmt", "log", "net/http")
      .map(doubleQuote)
      .mkString("\n")

    val customImportsBuffer: ListBuffer[String] = ListBuffer(
      s"${serviceName}DAO ${doubleQuote(s"${module}/dao")}",
    )
    if (usesComms) {
      customImportsBuffer += s"${serviceName}Comm ${doubleQuote(s"${module}/comm")}"
    }
    customImportsBuffer ++= Seq(
      doubleQuote(s"${module}/util"),
      s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
      doubleQuote("github.com/gorilla/mux"),
    )
    val customImports = customImportsBuffer.mkString("\n")

    sb.append(CodeWrap.parens.tabbed(standardImports + "\n\n" + customImports))
    sb.append("\n")
    sb.toString
  }

  /** Given a service name and whether the service uses inter-service communication, return global statements */
  private def generateGlobals(serviceName: String, usesComms: Boolean): String = {
    val globals: ListBuffer[String] = ListBuffer(
      s"var dao ${serviceName}DAO.DAO",
    )
    if (usesComms) globals += s"var comm ${serviceName}Comm.Handler"
    globals.mkString("", "\n", "\n")
  }

  /** Given a service name, whether the service uses inter-service communication, the endpoints desired and the port
    * number, generate the main function */
  private def generateMain(serviceName: String, usesComms: Boolean, endpoints: Set[Endpoint], port: Int): String = {
    val sb = new StringBuilder
    sb.append("func main() ")

    val bodyBuffer: ListBuffer[String] = ListBuffer(
      s"""configPtr := flag.String("config", "/etc/${serviceName}-service/config.json", "configuration filepath")""",
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
    )

    if (usesComms) {
      bodyBuffer ++= Seq(
        s"comm = ${serviceName}Comm.Handler{}",
        "comm.Init(config)",
        "",
      )
    }

    bodyBuffer += "r := mux.NewRouter()"
    if (endpoints.contains(Endpoint.ReadAll)) {
      bodyBuffer ++= Seq(
        "// Mux directs to first matching route, i.e. the order matters",
        s"""r.HandleFunc("/${serviceName}/all", ${serviceName}ListHandler).Methods(http.MethodGet)""",
      )
    }
    if (endpoints.contains(Endpoint.Create))
      bodyBuffer += s"""r.HandleFunc("/${serviceName}", ${serviceName}CreateHandler).Methods(http.MethodPost)"""
    if (endpoints.contains(Endpoint.Read))
      bodyBuffer += s"""r.HandleFunc("/${serviceName}/{id}", ${serviceName}ReadHandler).Methods(http.MethodGet)"""
    if (endpoints.contains(Endpoint.Update))
      bodyBuffer += s"""r.HandleFunc("/${serviceName}/{id}", ${serviceName}UpdateHandler).Methods(http.MethodPut)"""
    if (endpoints.contains(Endpoint.Delete))
      bodyBuffer += s"""r.HandleFunc("/${serviceName}/{id}", ${serviceName}DeleteHandler).Methods(http.MethodDelete)"""

    bodyBuffer ++= Seq(
      "r.Use(jsonMiddleware)",
      "",
      s"""log.Fatal(http.ListenAndServe(":${port}", r))""",
    )

    sb.append(CodeWrap.curly.tabbed(bodyBuffer.mkString("\n")))
    sb.append("\n")
    sb.toString
  }

  private def generateJsonMiddleware(): String =
    FileUtils.readFile("src/main/scala/temple/generate/language/service/go/genFiles/JsonMiddleware.go")

  private def generateHandler(serviceName: String, endpoint: Endpoint): String =
    s"func $serviceName${endpoint}Handler(w http.ResponseWriter, r *http.Request) {}\n"

  private def generateHandlers(serviceName: String, endpoints: Set[Endpoint]): String =
    (for (endpoint <- Endpoint.values if endpoints.contains(endpoint))
      yield generateHandler(serviceName, endpoint)).mkString("\n")

  private def generateErrors(serviceName: String): String = {
    val errors: ListBuffer[String] = ListBuffer(
      "package dao",
      "",
      """import "fmt"""",
      "",
      s"// Err${serviceName.capitalize}NotFound is returned when a ${serviceName} for the provided ID was not found",
      s"type Err${serviceName.capitalize}NotFound int64",
      "",
    )
    errors += Seq(
      s"func (e Err${serviceName.capitalize}NotFound) Error() string ",
      CodeWrap.curly.tabbed(s"""return fmt.Sprintf("${serviceName} not found with ID %d", e)"""),
    ).mkString("", "", "\n")
    errors.mkString("\n")
  }

  override def generate(serviceRoot: ServiceRoot): Map[FileUtils.File, FileUtils.FileContent] = {
    val usesComms = serviceRoot.comms.nonEmpty
    val serviceString = Seq(
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
    ).mkString("\n")
    val errorsString = generateErrors(serviceRoot.name)
    Map(
      FileUtils.File(serviceRoot.name, s"${serviceRoot.name}.go") -> serviceString,
      FileUtils.File(s"${serviceRoot.name}/dao", "errors.go")     -> errorsString,
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
