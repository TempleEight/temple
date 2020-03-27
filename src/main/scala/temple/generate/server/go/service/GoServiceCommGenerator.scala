package temple.generate.server.go.service

import temple.generate.server.ServiceRoot
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceCommGenerator {

  private[service] def generateImports(root: ServiceRoot): String = mkCode(
    "import",
    CodeWrap.parens.tabbed(
      s""""${root.module}/util"""",
    ),
  )

  private[service] def generateStructs(): String = mkCode.lines(
    "// Handler maintains the list of services and their associated hostnames",
    s"type Handler struct ${CodeWrap.curly.tabbed("Services map[string]string")}",
  )
}
