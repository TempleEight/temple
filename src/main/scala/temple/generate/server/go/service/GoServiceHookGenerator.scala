package temple.generate.server.go.service

import temple.generate.CRUD
import temple.generate.CRUD.CRUD
import temple.generate.CRUD.presentParticiple
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonHookGenerator
import temple.generate.utils.CodeTerm.mkCode

object GoServiceHookGenerator {

  private def generateBeforeHookType(root: ServiceRoot, operation: CRUD): String = operation match {
    case op @ (CRUD.List | CRUD.Read | CRUD.Delete) =>
      s"func(env *env, input *dao.${op.toString}${root.name.capitalize}Input) *HookError"
    case op @ (CRUD.Create | CRUD.Update) =>
      s"func(env *env, req ${op.toString.toLowerCase}${root.name.capitalize}Request, input *dao.${op.toString}${root.name.capitalize}Input) *HookError"
  }

  private def generateAfterHookType(root: ServiceRoot, operation: CRUD): String = operation match {
    case CRUD.List =>
      s"func(env *env, ${root.name}List *[]dao.${root.name.capitalize}) *HookError"
    case CRUD.Create | CRUD.Read | CRUD.Update =>
      s"func(env *env, ${root.name} *dao.${root.name.capitalize}) *HookError"
    case CRUD.Delete =>
      "func(env *env) *HookError"
  }

  private[service] def generateHookStruct(root: ServiceRoot, operations: Set[CRUD]): String = {
    val beforeCreate = operations.toSeq.sorted.map { op =>
      s"before${op.toString}Hooks" -> s"[]*${generateBeforeHookType(root, op)}"
    }

    val afterCreate = operations.toSeq.sorted.map { op =>
      s"after${op.toString}Hooks" -> s"[]*${generateAfterHookType(root, op)}"
    }

    mkCode.lines(
      GoCommonHookGenerator.generateHookStructComment,
      genStruct("Hook", beforeCreate ++ afterCreate),
    )
  }

  private def generateAddHookComment(action: String, op: CRUD): String = op match {
    case CRUD.List =>
      s"// ${action.capitalize}List adds a new hook to be executed ${action.toLowerCase} listing the objects in the datastore"
    case _ =>
      s"// ${action.capitalize}${op.toString.capitalize} adds a new hook to be executed ${action.toLowerCase} ${presentParticiple(op)} an object in the datastore"
  }

  private def generateAddHookMethod(action: String, hookType: String, op: CRUD): String =
    mkCode.lines(
      generateAddHookComment(action, op),
      genMethod(
        "h",
        "*Hook",
        s"${action.capitalize}${op.toString.capitalize}",
        Seq(s"hook $hookType"),
        Option.empty,
        genAssign(
          genFunctionCall("append", s"h.${action.toLowerCase}${op.toString.capitalize}Hooks", "&hook"),
          s"h.${action.toLowerCase}${op.toString.capitalize}Hooks",
        ),
      ),
    )

  private[service] def generateAddHookMethods(root: ServiceRoot, operations: Set[CRUD]): String = {
    val beforeHooks = operations.toSeq.sorted.map { op =>
      generateAddHookMethod("before", generateBeforeHookType(root, op), op)
    }

    val afterHooks = operations.toSeq.sorted.map { op =>
      generateAddHookMethod("after", generateAfterHookType(root, op), op)
    }

    mkCode.doubleLines(beforeHooks, afterHooks)
  }

}
