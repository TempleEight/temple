package temple.generate.server.go.service

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

object GoServiceUtilGenerator {

  private[go] def generateImports(): String = {
    val standardImports = Seq("encoding/json", "errors", "net/http", "os", "strings").map(doubleQuote)
    val customImports   = Seq("github.com/dgrijalva/jwt-go", "github.com/google/uuid").map(doubleQuote)

    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", customImports))
  }

  private[go] def generateAuthStruct(): String =
    FileUtils.readResources("go/genFiles/service/util/auth_struct.go.snippet").stripLineEnd

  private[go] def generateIDsFromRequest(): String =
    FileUtils.readResources("go/genFiles/service/util/ids_from_request.go.snippet").stripLineEnd
}
