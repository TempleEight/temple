package temple.generate.language.service.go

import temple.generate.language.service.ServiceGenerator
import temple.generate.language.service.adt._
import temple.generate.utils.CodeTerm
import temple.utils.FileUtils.{File, FileContent}
import temple.utils.StringUtils.tabIndent

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  private def generatePackage(packageName: String): String = s"package $packageName\n\n"

  /** Given a service name, module name and whether the service uses inter-service communication, return the import block */
  private def generateImports(serviceName: String, module: String, comms: Boolean): String = {
    val sb = new StringBuilder

    sb.append("import (" + "\n")
    sb.append(tabIndent(""""encoding/json"""") + "\n")
    sb.append(tabIndent(""""flag"""") + "\n")
    sb.append(tabIndent(""""fmt"""") + "\n")
    sb.append(tabIndent(""""log"""") + "\n")
    sb.append(tabIndent(""""net/http"""") + "\n")
    sb.append("\n")

    if (comms) {
      sb.append(tabIndent(s"""${serviceName}Comm "${module}/comm"""") + "\n")
    }

    sb.append(tabIndent(s"""${serviceName}DAO "${module}/dao"""") + "\n")
    sb.append(tabIndent(s""""${module}/util"""") + "\n")

    sb.append(tabIndent("""valid "github.com/asaskevich/govalidator"""") + "\n")
    sb.append(tabIndent(""""github.com/gorilla/mux"""") + "\n")
    sb.append(")" + "\n")
    sb.append("\n")

    sb.toString
  }

  override def generate(serviceRoot: ServiceRoot): Map[File, FileContent] = {
    val usesComms = serviceRoot.comms.length > 0
    val serviceString = generatePackage("main") + generateImports(
        serviceRoot.name,
        serviceRoot.module,
        usesComms,
      )
    Map(File(serviceRoot.name, s"${serviceRoot.name}.go") -> serviceString)
    /**
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
