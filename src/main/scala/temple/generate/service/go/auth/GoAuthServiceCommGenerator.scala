package temple.generate.service.go.auth

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceCommGenerator {

  private[auth] def generateImports(module: String): String = {
    val standardImports = Seq("encoding/json", "errors", "fmt", "io/ioutil", "net/http", "net/url").map(doubleQuote)
    val customImports   = doubleQuote(s"$module/util")
    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", customImports))
  }

  private[auth] def generateStructs(): String =
    FileUtils.readResources("go/genFiles/auth/handler_structs.go").stripLineEnd
}
