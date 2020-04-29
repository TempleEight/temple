package temple.generate.server.go.service

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoServiceUtilGenerator {

  private[service] def generateImports(): String = {
    val standardImports = Seq("encoding/json", "errors", "net/http", "os", "strings").map(doubleQuote)
    val customImports   = Seq("github.com/dgrijalva/jwt-go", "github.com/google/uuid").map(doubleQuote)

    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", customImports))
  }

  private[service] def generateAuthStruct(): String =
    FileUtils.readResources("go/genFiles/service/util/auth_struct.go.snippet").stripLineEnd

  private[service] def generateIDsFromRequest(hasStructs: Boolean): String =
    mkCode.doubleLines(
      FileUtils.readResources("go/genFiles/service/util/ids_from_request.go.snippet").stripLineEnd,
      when(hasStructs) {
        FileUtils.readResources("go/genFiles/service/util/struct_parent_id_from_request.go.snippet").stripLineEnd
      },
    )
}
