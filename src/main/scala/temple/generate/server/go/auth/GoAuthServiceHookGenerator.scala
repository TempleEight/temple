package temple.generate.server.go.auth

import temple.generate.CRUD
import temple.generate.CRUD.{CRUD, presentParticiple}
import temple.generate.server.go.common.GoCommonGenerator.{genAssign, genFunctionCall, genMethod, genStruct}
import temple.generate.server.go.common.GoCommonHookGenerator
import temple.generate.utils.CodeTerm.mkCode

object GoAuthServiceHookGenerator {
  private val endpoints = Seq("Register" -> CRUD.Create, "Login" -> CRUD.Read)

  private def generateBeforeHookType(action: String, operation: CRUD): String =
    s"func(env *env, req ${action.toLowerCase}AuthRequest, input *dao.${operation.toString}AuthInput) *HookError"

  private def generateAfterHookType: String =
    "func(env *env, auth *dao.Auth, accessToken string) *HookError"

  private[auth] def generateHookStruct: String = {
    val beforeCreate = endpoints.map {
      case (action, op) =>
        s"before${action}Hooks" -> s"[]*${generateBeforeHookType(action, op)}"
    }

    val afterCreate = endpoints.map {
      case (action, _) =>
        s"after${action}Hooks" -> s"[]*${generateAfterHookType}"
    }

    mkCode.lines(
      GoCommonHookGenerator.generateHookStructComment,
      genStruct("Hook", beforeCreate ++ afterCreate),
    )
  }

  private def generateAddHookComment(time: String, action: String, op: CRUD): String =
    s"// ${time.capitalize}${action.capitalize} adds a new hook to be executed ${time.toLowerCase} ${presentParticiple(op)} an object in the datastore"

  private def generateAddHookMethod(time: String, action: String, hookType: String, op: CRUD): String =
    mkCode.lines(
      generateAddHookComment(time, action, op),
      genMethod(
        "h",
        "*Hook",
        s"${time.capitalize}${action.capitalize}",
        Seq(s"hook $hookType"),
        Option.empty,
        genAssign(
          genFunctionCall("append", s"h.${time.toLowerCase}${action.capitalize}Hooks", "&hook"),
          s"h.${time.toLowerCase}${action.capitalize}Hooks",
        ),
      ),
    )

  private[auth] def generateAddHookMethods: String = {
    val beforeHooks = endpoints.map {
      case (action, op) =>
        generateAddHookMethod("before", action, generateBeforeHookType(action, op), op)
    }

    val afterHooks = endpoints.map {
      case (action, op) =>
        generateAddHookMethod("after", action, generateAfterHookType, op)
    }

    mkCode.doubleLines(beforeHooks, afterHooks)
  }

}
