package temple.generate.language.service.go

import temple.generate.language.service.ServiceGenerator
import temple.generate.language.service.adt._
import temple.utils.FileUtils.{File, FileContent}
import temple.utils.StringUtils.tabIndent

object GoServiceGenerator extends ServiceGenerator {

  private def generatePackage(packageName: String): String = s"package $packageName\n\n"

  private def generateImports(serviceName: String, module: String, comms: Boolean): String = {
    val sb = new StringBuilder

    sb.append("""import (
    |	"encoding/json"
    |	"flag"
    |	"fmt"
    |	"log"
    |	"net/http"""".stripMargin + "\n\n")

    if (comms) {
      sb.append(tabIndent(s"""${serviceName}Comm "${module}/comm"""") + "\n")
    }

    sb.append(tabIndent(s"""${serviceName}DAO "${module}/dao"""") + "\n")
    sb.append(tabIndent(s""""${module}/util"""") + "\n")

    sb.append("""	valid "github.com/asaskevich/govalidator"
    |	"github.com/gorilla/mux"
    |)""".stripMargin + "\n\n")

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
