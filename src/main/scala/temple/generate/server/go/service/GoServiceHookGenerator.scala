package temple.generate.server.go.service

import temple.ast.Metadata.Readable
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonHookGenerator
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoServiceHookGenerator {

  private[service] def hookSuffix(block: AttributesRoot): String =
    if (block.parentAttribute.nonEmpty) block.name else ""

  private[service] def generateImports(root: ServiceRoot): String = {
    val daoImport = s"${doubleQuote(root.module + "/dao")}"
    if (root.projectUsesAuth)
      mkCode("import", CodeWrap.parens.tabbed(daoImport, s"${doubleQuote(root.module + "/util")}"))
    else mkCode("import", daoImport)
  }

  private def generateHookType(block: AttributesRoot, args: String*): String =
    mkCode(
      CodeWrap.parens.prefix("func").list("env *env", args, when(block.projectUsesAuth) { "auth *util.Auth" }),
      "*HookError",
    )

  private def generateBeforeHookType(block: AttributesRoot, operation: CRUD): String =
    operation match {
      case List =>
        block.readable match {
          case Readable.This => generateHookType(block, s"input *dao.List${block.name}Input")
          case Readable.All  => generateHookType(block)
        }
      case op @ (Read | Delete | Identify) =>
        generateHookType(block, s"input *dao.${op.toString}${block.name}Input")
      case op @ (Create | Update) =>
        if (block.requestAttributes.isEmpty)
          generateHookType(block, s"input *dao.${op.toString}${block.name}Input")
        else
          generateHookType(
            block,
            s"req ${op.toString.toLowerCase}${block.name}Request",
            s"input *dao.${op.toString}${block.name}Input",
          )

    }

  private def generateAfterHookType(block: AttributesRoot, operation: CRUD): String = operation match {
    case List =>
      generateHookType(block, s"${block.decapitalizedName}List *[]dao.${block.name}")
    case Create | Read | Update | Identify =>
      generateHookType(block, s"${block.decapitalizedName} *dao.${block.name}")
    case Delete =>
      generateHookType(block)
  }

  private[service] def generateHookStruct(root: ServiceRoot): String = {
    val beforeCreate = root.blockIterator.map { block =>
      block.operations.toSeq.map { op =>
        s"before${op.toString}${hookSuffix(block)}Hooks" -> s"[]*${generateBeforeHookType(block, op)}"
      }
    }

    val afterCreate = root.blockIterator.map { block =>
      block.operations.toSeq.map { op =>
        s"after${op.toString}${hookSuffix(block)}Hooks" -> s"[]*${generateAfterHookType(block, op)}"
      }
    }

    mkCode.lines(
      GoCommonHookGenerator.generateHookStructComment,
      genStruct("Hook", (beforeCreate ++ afterCreate).toSeq: _*),
    )
  }

  private def generateAddHookComment(action: String, op: CRUD, structName: String): String = op match {
    case List =>
      s"// ${action.capitalize}List$structName adds a new hook to be executed ${action.toLowerCase} listing the objects in the datastore"
    case _ =>
      s"// ${action.capitalize}${op.toString.capitalize}$structName adds a new hook to be executed ${action.toLowerCase} ${presentParticiple(op)} an object in the datastore"
  }

  private def generateAddHookMethod(action: String, hookType: String, op: CRUD, structName: String): String =
    mkCode.lines(
      generateAddHookComment(action, op, structName),
      genMethod(
        "h",
        "*Hook",
        s"${action.capitalize}${op.toString.capitalize}$structName",
        Seq(s"hook $hookType"),
        None,
        genAssign(
          genFunctionCall("append", s"h.${action.toLowerCase}${op.toString.capitalize}${structName}Hooks", "&hook"),
          s"h.${action.toLowerCase}${op.toString.capitalize}${structName}Hooks",
        ),
      ),
    )

  private[service] def generateAddHookMethods(root: ServiceRoot): String = {
    val beforeHooks = root.blockIterator.flatMap { block =>
      block.operations.toSeq.map { op =>
        generateAddHookMethod("before", generateBeforeHookType(block, op), op, hookSuffix(block))
      }
    }

    val afterHooks = root.blockIterator.flatMap { block =>
      block.operations.toSeq.map { op =>
        generateAddHookMethod("after", generateAfterHookType(block, op), op, hookSuffix(block))
      }
    }

    mkCode.doubleLines(beforeHooks, afterHooks)
  }

}
