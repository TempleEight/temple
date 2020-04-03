package temple.generate.server.go.service.main

import temple.ast.Attribute
import temple.generate.CRUD.Create
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.generateHandlerDecl
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.server.go.GoHTTPStatus.{StatusBadRequest, StatusInternalServerError}

import scala.collection.immutable.ListMap
import scala.Option.when

object GoServiceMainCreateHandlerGenerator {

  /** Generate the checking that incoming request parameters are not nil */
  private def generateRequestNilCheck(root: ServiceRoot, clientAttributes: ListMap[String, Attribute]): String =
    genIf(
      clientAttributes.map { case name -> _ => s"req.${name.capitalize} == nil" }.mkString(" || "),
      generateHTTPErrorReturn(StatusBadRequest, "Missing request parameter(s)"),
    )

  /** Generate new UUID block */
  private def generateNewUUIDBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("uuid", "NewUUID"), "uuid", "err"),
      genIfErr(
        generateHTTPErrorReturn(StatusInternalServerError, "Could not create UUID: %s", genMethodCall("err", "Error")),
      ),
    )

  private def generateDAOCallBlock(
    root: ServiceRoot,
    hasAuthBlock: Boolean,
    clientAttributes: ListMap[String, Attribute],
  ): String = {
    val idCapitalized = root.idAttribute.name.toUpperCase
    // If service has auth block then an AuthID is passed in as ID, otherwise a created uuid is passed in
    val createInput = ListMap(idCapitalized -> (if (hasAuthBlock) s"auth.$idCapitalized" else "uuid")) ++
      // If service does not have auth block AuthID is passed for created_by field
      when(!hasAuthBlock) { s"Auth$idCapitalized" -> s"auth.$idCapitalized" } ++
      // TODO: ServerSet values need to be passed in, not just client-provided attributes
      clientAttributes.map { case str -> _ => str.capitalize -> s"*req.${str.capitalize}" }

    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"Create${root.name.capitalize}",
          genPopulateStruct(s"dao.Create${root.name.capitalize}Input", createInput),
        ),
        root.name,
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
  }

  /** Generate the create handler function */
  private[main] def generateCreateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, Attribute],
    usesComms: Boolean,
    hasAuthBlock: Boolean,
    responseMap: ListMap[String, String],
  ): String =
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          generateExtractAuthBlock(),
          generateDecodeRequestBlock(s"create${root.name.capitalize}"),
          generateRequestNilCheck(root, clientAttributes),
          generateValidateStructBlock(),
          when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes) },
          when(!hasAuthBlock) { generateNewUUIDBlock() },
          generateDAOCallBlock(root, hasAuthBlock, clientAttributes),
          generateJSONResponse(s"create${root.name.capitalize}", responseMap),
        ),
      ),
    )
}
