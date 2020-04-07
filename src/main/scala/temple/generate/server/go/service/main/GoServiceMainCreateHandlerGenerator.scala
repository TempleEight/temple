package temple.generate.server.go.service.main

import temple.ast.Attribute
import temple.generate.CRUD.Create
import temple.generate.server.ServiceRoot
import temple.generate.server.go.GoHTTPStatus.{StatusBadRequest, StatusInternalServerError}
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

  private def generateDAOCallBlock(
    root: ServiceRoot,
    clientAttributes: ListMap[String, Attribute],
  ): String = {
    val idCapitalized = root.idAttribute.name.toUpperCase
    // If service has auth block then an AuthID is passed in as ID, otherwise a created uuid is passed in
    val createInput = ListMap(idCapitalized -> (if (root.hasAuthBlock) s"auth.$idCapitalized" else "uuid")) ++
      // If the project uses auth, but this service does not have an auth block, AuthID is passed for created_by field
      when(!root.hasAuthBlock && root.projectUsesAuth) { s"Auth$idCapitalized" -> s"auth.$idCapitalized" } ++
      // TODO: ServerSet values need to be passed in, not just client-provided attributes
      clientAttributes.map { case str -> _ => str.capitalize -> s"*req.${str.capitalize}" }

    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"Create${root.name}",
          genPopulateStruct(s"dao.Create${root.name}Input", createInput),
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
  }

  /** Generate the create handler function */
  private[main] def generateCreateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, Attribute],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
  ): String =
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(usesVar = true) },
          generateDecodeRequestBlock(s"create${root.name}"),
          generateRequestNilCheck(root, clientAttributes),
          generateValidateStructBlock(),
          when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes) },
          when(!root.hasAuthBlock) { generateNewUUIDBlock() },
          generateDAOCallBlock(root, clientAttributes),
          generateJSONResponse(s"create${root.name}", responseMap),
        ),
      ),
    )
}
