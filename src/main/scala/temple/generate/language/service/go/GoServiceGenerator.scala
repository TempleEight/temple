package temple.generate.language.service.go

import temple.generate.language.service.ServiceGenerator
import temple.generate.language.service.adt._
import temple.generate.utils.CodeTerm.CodeWrap
import temple.utils.FileUtils.{File, FileContent}
import temple.utils.StringUtils.{doubleQuote, tabIndent}
import scala.collection.mutable.ListBuffer

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  private def generatePackage(packageName: String): String = s"package $packageName\n\n"

  /** Given a service name, module name and whether the service uses inter-service communication, return the import block */
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

  private def generateMain(serviceName: String, usesComms: Boolean, endpoints: Seq[Endpoint], port: Int): String = {
    val sb = new StringBuilder
    sb.append("func main() {\n")
    sb.append(
      tabIndent(
        s"""configPtr := flag.String("config", "/etc/${serviceName}-service/config.json", "configuration filepath")""" + "\n",
      ),
    )
    sb.append(tabIndent("flag.Parse()\n"))
    sb.append("\n")
    sb.append(tabIndent("// Require all struct fields by default\n"))
    sb.append(tabIndent("valid.SetFieldsRequiredByDefault(true)\n"))
    sb.append("\n")
    sb.append(tabIndent("config, err := utils.GetConfig(*configPtr)\n"))
    sb.append(tabIndent("if err != nil {\n"))
    sb.append(tabIndent("log.Fatal(err)\n", 2))
    sb.append(tabIndent("}\n"))
    sb.append("\n")
    sb.append(tabIndent(s"dao = ${serviceName}DAO.DAO{}\n"))
    sb.append(tabIndent("err = dao.Init(config)\n"))
    sb.append(tabIndent("if err != nil {\n"))
    sb.append(tabIndent("log.Fatal(err)\n", 2))
    sb.append(tabIndent("}\n"))
    sb.append("\n")
    if (usesComms) {
      sb.append(tabIndent(s"comm = ${serviceName}Comm.Handler{}\n"))
      sb.append(tabIndent("comm.Init(config)\n"))
      sb.append("\n")
    }
    sb.append(tabIndent("r := mux.NewRouter()\n"))
    if (endpoints.contains(Endpoint.All)) {
      sb.append(tabIndent("// Mux directs to first matching route, i.e. the order matters\n"))
      sb.append(
        tabIndent(s"""r.HandleFunc("/${serviceName}/all", ${serviceName}ListHandler).Methods(http.MethodGet)""" + "\n"),
      )
    }
    if (endpoints.contains(Endpoint.Create)) {
      sb.append(
        tabIndent(s"""r.HandleFunc("/${serviceName}", ${serviceName}CreateHandler).Methods(http.MethodPost)""" + "\n"),
      )
    }
    if (endpoints.contains(Endpoint.Read)) {
      sb.append(
        tabIndent(
          s"""r.HandleFunc("/${serviceName}/{id}", ${serviceName}ReadHandler).Methods(http.MethodGet)""" + "\n",
        ),
      )
    }
    if (endpoints.contains(Endpoint.Update)) {
      sb.append(
        tabIndent(
          s"""r.HandleFunc("/${serviceName}/{id}", ${serviceName}UpdateHandler).Methods(http.MethodPut)""" + "\n",
        ),
      )
    }
    if (endpoints.contains(Endpoint.Delete)) {
      sb.append(
        tabIndent(
          s"""r.HandleFunc("/${serviceName}/{id}", ${serviceName}DeleteHandler).Methods(http.MethodDelete)""" + "\n",
        ),
      )
    }
    sb.append(tabIndent("r.Use(jsonMiddleware)\n"))
    sb.append("\n")
    sb.append(tabIndent(s"""log.Fatal(http.ListenAndServe(":${port}", r))""" + "\n"))
    sb.append("}\n\n")

    sb.toString
  }

  override def generate(serviceRoot: ServiceRoot): Map[File, FileContent] = {
    val usesComms = serviceRoot.comms.nonEmpty
    val serviceString = generatePackage("main") + generateImports(
        serviceRoot.name,
        serviceRoot.module,
        usesComms,
      ) + generateMain(
        serviceRoot.name,
        usesComms,
        serviceRoot.endpoints,
        serviceRoot.port,
      )
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
