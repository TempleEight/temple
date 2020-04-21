package temple.generate.server.go.common

import temple.generate.server.go.common.GoCommonGenerator.{genMethod, genReturn, genStruct}
import temple.generate.utils.CodeTerm.mkCode

import scala.collection.immutable.ListMap

object GoCommonHookGenerator {

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

  private[go] def generateHookStructComment: String =
    mkCode.lines(
      "// Hook allows additional code to be executed before and after every datastore interaction",
      "// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated",
    )
}
