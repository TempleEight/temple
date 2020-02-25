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
  private def generateImports(serviceName: String, module: String, comms: Boolean): String = {
    val sb = new StringBuilder
    sb.append("import ")

    val standardImports = Seq("encoding/json", "flag", "fmt", "log", "net/http")
      .map(doubleQuote)
      .mkString("\n")

    var customImportsBuffer: ListBuffer[String] = ListBuffer.empty
    if (comms) {
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

  override def generate(serviceRoot: ServiceRoot): Map[File, FileContent] = {
    val usesComms = serviceRoot.comms.nonEmpty
    val serviceString = generatePackage("main") + generateImports(
        serviceRoot.name,
        serviceRoot.module,
        usesComms,
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
