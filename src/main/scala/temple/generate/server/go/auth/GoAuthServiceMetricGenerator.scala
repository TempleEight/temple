package temple.generate.server.go.auth

import temple.generate.server.go.common.GoCommonMetricGenerator
import temple.generate.utils.CodeUtils
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceMetricGenerator {

  private[auth] def generateVars(): String = {
    val serviceGlobals = CodeUtils.pad(Seq("register", "login").map { operation =>
      (s"Request${operation.capitalize}", doubleQuote(operation.toLowerCase))
    }, separator = " = ")

    GoCommonMetricGenerator.generateVars(serviceGlobals, "auth")
  }
}
