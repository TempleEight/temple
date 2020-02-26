package temple.generate.language.service.go

import temple.generate.language.service.ServiceGenerator
import temple.generate.language.service.adt._
import temple.generate.utils.CodeTerm.CodeWrap
import temple.utils.FileUtils._
import temple.utils.StringUtils.{doubleQuote, tabIndent}
import scala.collection.mutable.ListBuffer
import temple.utils.FileUtils

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  private def generatePackage(packageName: String): String = s"package $packageName\n\n"

  /** Given a service name, module name and whether the service uses inter-service communication, return the import
    * block */
  private def generateImports(serviceName: String, module: String, usesComms: Boolean): String = {
    val sb = new StringBuilder
    sb.append("import ")

    val standardImports = Seq("encoding/json", "flag", "fmt", "log", "net/http")
      .map(doubleQuote)
      .mkString("\n")

    var customImportsBuffer: ListBuffer[String] = ListBuffer.empty
    if (usesComms) {
      customImportsBuffer += s"${serviceName}Comm ${doubleQuote(s"${module}/comm")}"
    }
    customImportsBuffer ++= Seq(
      s"${serviceName}DAO ${doubleQuote(s"${module}/dao")}",
      doubleQuote(s"${module}/util"),
      s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
      doubleQuote("github.com/gorilla/mux"),
    )
    val customImports = customImportsBuffer.mkString("\n")

    sb.append(CodeWrap.parens.tabbed(standardImports + "\n\n" + customImports))
    sb.append("\n\n")
    sb.toString
  }

  /** Given a service name and whether the service uses inter-service communication, return global statements */
  private def generateGlobals(serviceName: String, usesComms: Boolean): String = {
    val sb = new StringBuilder
    sb.append(s"var dao ${serviceName}DAO.DAO\n")
    if (usesComms) sb.append(s"var comm ${serviceName}Comm.Handler\n")
    sb.append("\n")
    sb.toString
  }

  /** Given a service name, whether the service uses inter-service communication, the endpoints desired and the port
    * number, generate the main function */
  private def generateMain(serviceName: String, usesComms: Boolean, endpoints: Set[Endpoint], port: Int): String = {
    val sb = new StringBuilder
    sb.append("func main() ")

    var bodyBuffer: ListBuffer[String] = ListBuffer(
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
    sb.append("\n\n")
    sb.toString
  }

  private def generateJsonMiddleware(): String =
    FileUtils.readFile("src/main/scala/temple/generate/language/service/go/genFiles/JsonMiddleware.go")

  override def generate(serviceRoot: ServiceRoot): Map[File, FileContent] = {
    val usesComms = serviceRoot.comms.nonEmpty
    val serviceString = generatePackage("main") + generateImports(
        serviceRoot.name,
        serviceRoot.module,
        usesComms,
      ) + generateGlobals(
        serviceRoot.name,
        usesComms,
      ) + generateMain(
        serviceRoot.name,
        usesComms,
        serviceRoot.endpoints,
        serviceRoot.port,
      ) + generateJsonMiddleware()
    Map(File(serviceRoot.name, s"${serviceRoot.name}.go") -> serviceString)
    /*
   * TODO:
   * <>.go
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
