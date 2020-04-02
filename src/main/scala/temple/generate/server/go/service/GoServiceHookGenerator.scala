package temple.generate.server.go.service

import temple.generate.CRUD
import temple.generate.CRUD.CRUD
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator.{genStruct, genMethod, genReturn}
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.doubleQuote

import scala.collection.immutable.ListMap

object GoServiceHookGenerator {

  private[service] def generateImports(root: ServiceRoot): String =
    s"import ${doubleQuote(root.module + "/dao")}"

  private[service] def generateHookStruct(root: ServiceRoot, operations: Set[CRUD]): String = {
    val beforeCreate = operations.toSeq.sorted.map {
      case op @ (CRUD.List | CRUD.Read | CRUD.Delete) =>
        s"before${op.toString}Hooks" -> s"[]*func(env *env, input *dao.${op.toString}${root.name}Input) *HookError"
      case op @ (CRUD.Create | CRUD.Update) =>
        s"before${op.toString}Hooks" -> s"[]*func(env *env, req ${op.toString.toLowerCase}${root.name}Request, input *dao.${op.toString}${root.name}Input) *HookError"
    }

    val afterCreate = operations.toSeq.sorted.map {
      case CRUD.List =>
        "afterListHooks" -> s"[]*func(env *env, ${root.name}List *[]dao.${root.name}) *HookError"
      case op @ (CRUD.Create | CRUD.Read | CRUD.Update) =>
        s"after${op.toString}Hooks" -> s"[]*func(env *env, ${root.decapitalizedName} *dao.${root.name}) *HookError"
      case CRUD.Delete =>
        s"afterDeleteHooks" -> s"[]*func(env *env) *HookError"
    }

    mkCode.lines(
      "// Hook allows additional code to be executed before and after every datastore interaction",
      "// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated",
      genStruct("Hook", beforeCreate ++ afterCreate),
    )
  }

  private[service] def generateHookErrorStruct: String =
    mkCode.lines(
      "// HookError wraps an existing error with HTTP status code",
      genStruct("HookError", ListMap("statusCode" -> "int", "error" -> "error")),
    )

  private[service] def generateHookErrorFunction: String =
    genMethod(
      "e",
      "*HookError",
      "Error",
      Seq.empty,
      Some("string"),
      genReturn("e.error.Error()"),
    )
}
