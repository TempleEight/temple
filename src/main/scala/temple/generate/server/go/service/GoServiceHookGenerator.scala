package temple.generate.server.go.service

import temple.generate.server.ServiceRoot
import temple.utils.StringUtils.doubleQuote

object GoServiceHookGenerator {

  private[service] def generateImports(root: ServiceRoot): String =
    s"import ${doubleQuote(root.module + "/dao")}"
}
