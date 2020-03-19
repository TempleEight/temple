package temple.generate.server.go.service

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceCommGenerator {

  private[service] def generateImports(module: String): String = mkCode(
    "import",
    CodeWrap.parens.tabbed(
      s""""$module/util"""",
    ),
  )

  private[service] def generateStructs(): String = mkCode.lines(
    "// Handler maintains the list of services and their associated hostnames",
    s"type Handler struct ${CodeWrap.curly.tabbed("Services map[string]string")}",
  )
}
