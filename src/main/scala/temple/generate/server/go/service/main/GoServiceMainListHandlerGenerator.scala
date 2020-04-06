package temple.generate.server.go.service.main

import temple.generate.CRUD.List
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.{generateHandlerDecl, generateExtractAuthBlock, generateHTTPError}
import temple.generate.server.{CreatedByAttribute, ServiceRoot}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.server.go.GoHTTPStatus.StatusInternalServerError

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainListHandlerGenerator {

  /** Generate the list handler function */
  private[main] def generateListHandler(
    root: ServiceRoot,
    responseMap: ListMap[String, String],
    enumeratingByCreator: Boolean,
  ): String = {
    // Fetch list from DAO
    val queryDAOBlock =
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"List${root.name}",
          when(enumeratingByCreator) {
            genPopulateStruct(
              s"dao.List${root.name}Input",
              ListMap(s"AuthID" -> "auth.ID"),
            )
          },
        ),
        s"${root.decapitalizedName}List",
        "err",
      )

    // Check error from fetching list from DAO
    val queryDAOErrorBlock = genIfErr(
      mkCode.lines(
        generateHTTPError(
          StatusInternalServerError,
          "Something went wrong: %s",
          genMethodCall("err", "Error"),
        ),
        genReturn(),
      ),
    )

    // Instantiate list response object
    val instantiateResponseBlock = genDeclareAndAssign(
      genPopulateStruct(
        s"list${root.name}Response",
        ListMap(
          s"${root.name}List" -> genFunctionCall("make", s"[]list${root.name}Element", "0"),
        ),
      ),
      s"${root.decapitalizedName}ListResp",
    )

    // Map DAO result into response object
    val mapResponseBlock = genForLoop(
      genDeclareAndAssign(s"range *${root.decapitalizedName}List", "_", root.decapitalizedName),
      genAssign(
        genFunctionCall(
          "append",
          s"${root.decapitalizedName}ListResp.${root.name}List",
          genPopulateStruct(s"list${root.name}Element", responseMap),
        ),
        s"${root.decapitalizedName}ListResp.${root.name}List",
      ),
    )

    mkCode(
      generateHandlerDecl(root, List),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(enumeratingByCreator) { generateExtractAuthBlock() },
          mkCode.lines(
            queryDAOBlock,
            queryDAOErrorBlock,
          ),
          instantiateResponseBlock,
          mapResponseBlock,
          genMethodCall(genMethodCall("json", "NewEncoder", "w"), "Encode", s"${root.decapitalizedName}ListResp"),
        ),
      ),
    )
  }

}
