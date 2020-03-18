package temple.generate.service.go.auth

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceUtilGenerator {

  private[auth] def generateImports(): String =
    mkCode("import", CodeWrap.parens.tabbed(Seq("encoding/json", "os").map(doubleQuote)))
}
