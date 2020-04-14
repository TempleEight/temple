package temple.generate.server.go.service.main

import temple.generate.CRUD.List
import temple.generate.server.ServiceRoot
import temple.generate.server.go.GoHTTPStatus.StatusInternalServerError
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainListHandlerGenerator {

  // TODO: This needs a refactor
  /** Generate the list handler function */
  private[main] def generateListHandler(
    root: ServiceRoot,
    responseMap: ListMap[String, String],
    enumeratingByCreator: Boolean,
    usesMetrics: Boolean,
  ): String = {
    val queryDAOInputBlock = when(enumeratingByCreator) {
      genDeclareAndAssign(
        genPopulateStruct(
          s"dao.List${root.name}Input",
          ListMap(s"AuthID" -> "auth.ID"),
        ),
        "input",
      )
    }

    // Fetch list from DAO
    val queryDAOBlock =
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"List${root.name}",
          when(enumeratingByCreator) {
            "input"
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
          when(enumeratingByCreator) { generateExtractAuthBlock(usesVar = true) },
          mkCode.doubleLines(
            queryDAOInputBlock,
            generateInvokeBeforeHookBlock(root, ListMap(), List),
            mkCode.lines(
              when(usesMetrics) { generateMetricTimerDecl(List.toString) },
              queryDAOBlock,
              when(usesMetrics) { generateMetricTimerObservation() },
              queryDAOErrorBlock,
            ),
          ),
          instantiateResponseBlock,
          mapResponseBlock,
          genMethodCall(genMethodCall("json", "NewEncoder", "w"), "Encode", s"${root.decapitalizedName}ListResp"),
          when(usesMetrics) { generateMetricSuccess(List.toString) },
        ),
      ),
    )
  }

}
