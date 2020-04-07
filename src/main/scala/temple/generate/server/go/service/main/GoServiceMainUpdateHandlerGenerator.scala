package temple.generate.server.go.service.main

import temple.ast.Attribute
import temple.ast.Metadata.Writable
import temple.generate.CRUD.Update
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainUpdateHandlerGenerator {

  private def generateDAOCallBlock(root: ServiceRoot, clientAttributes: ListMap[String, Attribute]): String = {
    val createInput = ListMap("ID" -> s"${root.decapitalizedName}ID") ++
      clientAttributes.map { case str -> _ => str.capitalize -> s"*req.${str.capitalize}" }
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("env.dao", s"Update${root.name}", genPopulateStruct(s"dao.Update${root.name}Input", createInput)),
        root.decapitalizedName,
        "err",
      ),
      generateDAOCallErrorBlock(root),
    )
  }

  /** Generate the update handler function */
  private[main] def generateUpdateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, Attribute],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
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
              generateDecodeRequestBlock(s"update${root.name}"),
              generateRequestNilCheck(root, clientAttributes),
              generateValidateStructBlock(),
              when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes) },
            )
          },
          generateDAOCallBlock(root, clientAttributes),
          generateJSONResponse(s"update${root.name}", responseMap),
        ),
      ),
    )
}
