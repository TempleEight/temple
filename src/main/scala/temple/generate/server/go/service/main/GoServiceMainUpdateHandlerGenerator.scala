package temple.generate.server.go.service.main

import temple.ast.AbstractAttribute
import temple.ast.Metadata.Writable
import temple.generate.CRUD.Update
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainUpdateHandlerGenerator {

  private def generateDAOInput(root: ServiceRoot, clientAttributes: ListMap[String, AbstractAttribute]): String = {
    val updateInput = ListMap("ID" -> s"${root.decapitalizedName}ID") ++ generateDAOInputClientMap(clientAttributes)
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Update${root.name}Input", updateInput),
      "input",
    )
  }

  private def generateDAOCallBlock(root: ServiceRoot): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("env.dao", s"Update${root.name}", "input"),
        root.decapitalizedName,
        "err",
      ),
      generateDAOCallErrorBlock(root),
    )

  /** Generate the update handler function */
  private[main] def generateUpdateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
  ): String =
    mkCode(
      generateHandlerDecl(root, Update),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(root.writable == Writable.This) },
          generateExtractIDBlock(root.decapitalizedName),
          when(root.writable == Writable.This) { generateCheckAuthorizationBlock(root) },
          // Only need to handle request JSONs when there are client attributes
          when(clientAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(root, Update, s"update${root.name}"),
              generateRequestNilCheck(root, clientAttributes),
              generateValidateStructBlock(),
              when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes) },
              when(clientUsesTime) { generateParseTimeBlocks(clientAttributes) },
            )
          },
          generateDAOInput(root, clientAttributes),
          generateInvokeBeforeHookBlock(root, clientAttributes, Update),
          generateDAOCallBlock(root),
          generateJSONResponse(s"update${root.name}", responseMap),
        ),
      ),
    )
}
