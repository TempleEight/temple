package temple.generate.server.go.service.main

import temple.ast.AbstractAttribute
import temple.ast.Metadata.Writable
import temple.generate.CRUD.Delete
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainDeleteHandlerGenerator {

  private def generateDAOInput(root: ServiceRoot): String =
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Delete${root.name}Input", ListMap("ID" -> s"${root.decapitalizedName}ID")),
      "input",
    )

  private def generateDAOCallBlock(root: ServiceRoot): String =
    mkCode.lines(
      genAssign(
        genMethodCall(
          "env.dao",
          s"Delete${root.name}",
          "input",
        ),
        "err",
      ),
      generateDAOCallErrorBlock(root),
    )

  /** Generate the delete handler function */
  private[main] def generateDeleteHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Delete),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(root.writable == Writable.This) },
          generateExtractIDBlock(root.decapitalizedName),
          when(root.writable == Writable.This) { generateCheckAuthorizationBlock(root) },
          generateDAOInput(root),
          generateDAOCallBlock(root),
          genMethodCall(genMethodCall("json", "NewEncoder", "w"), "Encode", "struct{}{}"),
        ),
      ),
    )
}
