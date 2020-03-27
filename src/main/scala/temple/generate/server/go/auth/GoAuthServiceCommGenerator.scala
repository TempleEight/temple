package temple.generate.server.go.auth

import temple.generate.server.AuthServiceRoot
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceCommGenerator {

  private[auth] def generateImports(root: AuthServiceRoot): String = {
    val standardImports = Seq("encoding/json", "errors", "fmt", "io/ioutil", "net/http", "net/url").map(doubleQuote)
    val customImports   = doubleQuote(s"${root.module}/util")
    mkCode("import", CodeWrap.parens.tabbed(standardImports, "", customImports))
  }

  private[auth] def generateStructs(): String =
    FileUtils.readResources("go/genFiles/auth/comm/structs.go.snippet").stripLineEnd

  private[auth] def generateCreateJWTCredential(): String =
    FileUtils.readResources("go/genFiles/auth/comm/create_jwt_credential.go.snippet").stripLineEnd
}
