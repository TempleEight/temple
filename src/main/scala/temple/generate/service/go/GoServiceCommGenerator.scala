package temple.generate.service.go

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils

object GoServiceCommGenerator {

  private[go] def generateImports(module: String): String = mkCode(
    "import",
    CodeWrap.parens.tabbed(
      s""""$module/util"""",
    ),
  )

  private[go] def generateStructs(): String = mkCode.lines(
    "// Handler maintains the list of services and their associated hostnames",
    s"type Handler struct ${CodeWrap.curly.tabbed("Services map[string]string")}",
  )
}
