package temple.generate.server.go.service

import temple.ast.Metadata.Readable
import temple.generate.CRUD
import temple.generate.CRUD.{CRUD, presentParticiple}
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonHookGenerator
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoServiceHookGenerator {

  private[service] def generateImports(root: ServiceRoot): String = {
    val daoImport = s"${doubleQuote(root.module + "/dao")}"
    if (root.projectUsesAuth)
      mkCode("import", CodeWrap.parens.tabbed(daoImport, s"${doubleQuote(root.module + "/util")}"))
    else mkCode("import", daoImport)
  }

  private def generateHookType(root: ServiceRoot, args: String*): String =
    mkCode(
      CodeWrap.parens
        .prefix("func")
        .list("env *env", args, when(root.projectUsesAuth) { "auth *util.Auth" }),
      "*HookError",
    )

  private def generateBeforeHookType(root: ServiceRoot, operation: CRUD): String =
    operation match {
      case CRUD.List =>
        root.readable match {
          case Readable.This => generateHookType(root, s"input *dao.List${root.name}Input")
          case Readable.All  => generateHookType(root)
        }
      case op @ (CRUD.Read | CRUD.Delete | CRUD.Identify) =>
        generateHookType(root, s"input *dao.${op.toString}${root.name}Input")
      case op @ (CRUD.Create | CRUD.Update) =>
        if (root.requestAttributes.isEmpty)
          generateHookType(root, s"input *dao.${op.toString}${root.name}Input")
        else
          generateHookType(
            root,
            s"req ${op.toString.toLowerCase}${root.name}Request",
            s"input *dao.${op.toString}${root.name}Input",
          )

    }

  private def generateAfterHookType(root: ServiceRoot, operation: CRUD): String =
    operation match {
      case CRUD.List =>
        generateHookType(root, s"${root.decapitalizedName}List *[]dao.${root.name}")
      case CRUD.Create | CRUD.Read | CRUD.Update | CRUD.Identify =>
        generateHookType(root, s"${root.decapitalizedName} *dao.${root.name}")
      case CRUD.Delete =>
        generateHookType(root)
    }

  private[service] def generateHookStruct(root: ServiceRoot): String = {
    val beforeCreate = root.operations.toSeq.map { op =>
      s"before${op.toString}Hooks" -> s"[]*${generateBeforeHookType(root, op)}"
    }

    val afterCreate = root.operations.toSeq.map { op =>
      s"after${op.toString}Hooks" -> s"[]*${generateAfterHookType(root, op)}"
    }

    mkCode.lines(
      GoCommonHookGenerator.generateHookStructComment,
      genStruct("Hook", beforeCreate, afterCreate),
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

  private[service] def generateAddHookMethods(root: ServiceRoot): String = {
    val beforeHooks = root.operations.toSeq.map { op =>
      generateAddHookMethod("before", generateBeforeHookType(root, op), op)
    }

    val afterHooks = root.operations.toSeq.map { op =>
      generateAddHookMethod("after", generateAfterHookType(root, op), op)
    }

    mkCode.doubleLines(beforeHooks, afterHooks)
  }

}
