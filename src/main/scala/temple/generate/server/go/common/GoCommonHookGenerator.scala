package temple.generate.server.go.common

import temple.generate.server.go.common.GoCommonGenerator.{genMethod, genReturn, genStruct}
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.doubleQuote

import scala.collection.immutable.ListMap

object GoCommonHookGenerator {

  private[go] def generateImports(rootModule: String): String =
    s"import ${doubleQuote(rootModule + "/dao")}"

  private[go] def generateHookErrorStruct: String =
    mkCode.lines(
      "// HookError wraps an existing error with HTTP status code",
      genStruct("HookError", ListMap("statusCode" -> "int", "error" -> "error")),
    )

  private[go] def generateHookErrorFunction: String =
    genMethod(
      "e",
      "*HookError",
      "Error",
      Seq.empty,
      Some("string"),
      genReturn("e.error.Error()"),
    )
}
