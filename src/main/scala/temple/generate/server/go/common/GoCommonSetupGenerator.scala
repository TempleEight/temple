package temple.generate.server.go.common

import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils

object GoCommonSetupGenerator {

  def generateImports: String =
    mkCode("import", StringUtils.doubleQuote("github.com/gorilla/mux"))

  def generateSetupMethod: String =
    GoCommonGenerator.genMethod(
      objectName = "env",
      objectType = "*env",
      methodName = "setup",
      methodArgs = Seq("router *mux.Router"),
      methodReturn = None,
      methodBody = "// Add user defined code here",
    )
}
