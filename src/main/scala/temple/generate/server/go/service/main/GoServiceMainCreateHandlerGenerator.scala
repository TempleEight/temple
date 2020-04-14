package temple.generate.server.go.service.main

import temple.ast.AbstractAttribute
import temple.generate.CRUD.Create
import temple.generate.server.ServiceRoot
import temple.generate.server.go.GoHTTPStatus.StatusInternalServerError
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.{generateHandlerDecl, _}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainCreateHandlerGenerator {

  /** Generate new UUID block */
  private def generateNewUUIDBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("uuid", "NewUUID"), "uuid", "err"),
      genIfErr(
        generateHTTPErrorReturn(StatusInternalServerError, "Could not create UUID: %s", genMethodCall("err", "Error")),
      ),
    )

  private def generateDAOInput(root: ServiceRoot, clientAttributes: ListMap[String, AbstractAttribute]): String = {
    val idCapitalized = root.idAttribute.name.toUpperCase
    // If service has auth block then an AuthID is passed in as ID, otherwise a created uuid is passed in
    val createInput = ListMap(idCapitalized -> (if (root.hasAuthBlock) s"auth.$idCapitalized" else "uuid")) ++
      // If the project uses auth, but this service does not have an auth block, AuthID is passed for created_by field
      when(!root.hasAuthBlock && root.projectUsesAuth) { s"Auth$idCapitalized" -> s"auth.$idCapitalized" } ++
      generateDAOInputClientMap(clientAttributes)

    genDeclareAndAssign(
      genPopulateStruct(s"dao.Create${root.name}Input", createInput),
      "input",
    )
  }

  private def generateDAOCallBlock(root: ServiceRoot): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"Create${root.name}",
          "input",
        ),
        root.decapitalizedName,
        "err",
      ),
      genIfErr(
        generateHTTPErrorReturn(
          StatusInternalServerError,
          "Something went wrong: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  /** Generate the create handler function */
  private[main] def generateCreateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
  ): String =
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(usesVar = true) },
          // Only need to handle request JSONs when there are client attributes
          when(clientAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(root, Create, s"create${root.name}"),
              generateRequestNilCheck(root, clientAttributes),
              generateValidateStructBlock(),
              when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes) },
              when(clientUsesTime) { generateParseTimeBlocks(clientAttributes) },
            )
          },
          when(!root.hasAuthBlock) { generateNewUUIDBlock() },
          generateDAOInput(root, clientAttributes),
          generateInvokeBeforeHookBlock(root, clientAttributes, Create),
          generateDAOCallBlock(root),
          generateJSONResponse(s"create${root.name}", responseMap),
        ),
      ),
    )
}
