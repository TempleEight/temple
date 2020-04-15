package temple.generate.server.go.service

import temple.ast.AbstractAttribute
import temple.ast.Metadata.Readable
import temple.generate.CRUD
import temple.generate.CRUD.{CRUD, presentParticiple}
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonHookGenerator
import temple.generate.utils.CodeTerm.mkCode

import scala.collection.immutable.ListMap

object GoServiceHookGenerator {

  private def generateBeforeHookType(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    operation: CRUD,
  ): String = operation match {
    case CRUD.List =>
      root.readable match {
        case Readable.This =>
          s"func(env *env, input *dao.List${root.name}Input) *HookError"
        case Readable.All =>
          s"func(env *env) *HookError"
      }
    case op @ (CRUD.Read | CRUD.Delete) =>
      s"func(env *env, input *dao.${op.toString}${root.name}Input) *HookError"
    case op @ (CRUD.Create | CRUD.Update) =>
      if (clientAttributes.isEmpty)
        s"func(env *env, input *dao.${op.toString}${root.name}Input) *HookError"
      else
        s"func(env *env, req ${op.toString.toLowerCase}${root.name}Request, input *dao.${op.toString}${root.name}Input) *HookError"

  }

  private def generateAfterHookType(root: ServiceRoot, operation: CRUD): String = operation match {
    case CRUD.List =>
      s"func(env *env, ${root.decapitalizedName}List *[]dao.${root.name}) *HookError"
    case CRUD.Create | CRUD.Read | CRUD.Update =>
      s"func(env *env, ${root.decapitalizedName} *dao.${root.name}) *HookError"
    case CRUD.Delete =>
      "func(env *env) *HookError"
  }

  private[service] def generateHookStruct(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    operations: Set[CRUD],
  ): String = {
    val beforeCreate = operations.toSeq.sorted.map { op =>
      s"before${op.toString}Hooks" -> s"[]*${generateBeforeHookType(root, clientAttributes, op)}"
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

  private[service] def generateAddHookMethods(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    operations: Set[CRUD],
  ): String = {
    val beforeHooks = operations.toSeq.sorted.map { op =>
      generateAddHookMethod("before", generateBeforeHookType(root, clientAttributes, op), op)
    }

    val afterHooks = operations.toSeq.sorted.map { op =>
      generateAddHookMethod("after", generateAfterHookType(root, op), op)
    }

    mkCode.doubleLines(beforeHooks, afterHooks)
  }

}
