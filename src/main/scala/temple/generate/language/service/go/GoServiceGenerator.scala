package temple.generate.language.service.go

import temple.generate.language.service.ServiceGenerator
import temple.generate.language.service.adt._
import temple.generate.utils.CodeTerm._
import temple.utils.FileUtils.{File, FileContent}
import temple.utils.StringUtils.tabIndent
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
      .map(tabIndent(_))
      .map(_ + "\n")

    var customImports: ListBuffer[String] = ListBuffer.empty
    if (comms) {
      customImports += s"${serviceName}Comm ${doubleQuote(s"${module}/comm")}"
    }
    customImports ++= Seq(
      s"${serviceName}DAO ${doubleQuote(s"${module}/dao")}",
      doubleQuote(s"${module}/util"),
      s"valid ${doubleQuote("github.com/asaskevich/govalidator")}",
      doubleQuote("github.com/gorilla/mux"),
    )
    customImports = customImports.map(tabIndent(_)).map(_ + "\n")

    sb.append(codeWrap.parens.block(Seq(standardImports.mkString, "\n", customImports.mkString).mkString))
    sb.append("\n")

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
