package temple.generate.server.go.service.main

import temple.ast.Metadata.Readable
import temple.generate.CRUD.List
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot.ServiceRoot
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
    block: AttributesRoot,
    parent: Option[ServiceRoot],
    responseMap: ListMap[String, String],
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { List.toString + block.structName }
    val daoInput = ListMap.from(
      Iterator()
      ++ when(block.readable == Readable.This && parent.isEmpty) { "AuthID" -> "auth.ID" }
      ++ parent.map(parent => "ParentID"                                    -> s"${parent.decapitalizedName}ID"),
    )
    val queryDAOInputBlock = when(daoInput.nonEmpty) {
      genDeclareAndAssign(
        genPopulateStruct(
          s"dao.List${block.name}Input",
          daoInput,
        ),
        "input",
      )
    }

    // Fetch list from DAO
    val queryDAOBlock =
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"List${block.name}",
          when(block.readable == Readable.This || parent.nonEmpty) { "input" },
        ),
        s"${block.decapitalizedName}List",
        "err",
      )

    // Check error from fetching list from DAO
    val queryDAOErrorBlock = genIfErr(
      mkCode.lines(
        generateRespondWithError(
          genHTTPEnum(StatusInternalServerError),
          metricSuffix,
          "Something went wrong: %s",
          genMethodCall("err", "Error"),
        ),
        genReturn(),
      ),
    )

    // Instantiate list response object
    val instantiateResponseBlock = genDeclareAndAssign(
      genPopulateStruct(
        s"list${block.name}Response",
        ListMap(
          s"${block.name}List" -> genFunctionCall("make", s"[]list${block.name}Element", "0"),
        ),
      ),
      s"${block.decapitalizedName}ListResp",
    )

    // Map DAO result into response object
    val mapResponseBlock = genForLoop(
      genDeclareAndAssign(s"range *${block.decapitalizedName}List", "_", block.decapitalizedName),
      genAssign(
        genFunctionCall(
          "append",
          s"${block.decapitalizedName}ListResp.${block.name}List",
          genPopulateStruct(s"list${block.name}Element", responseMap),
        ),
        s"${block.decapitalizedName}ListResp.${block.name}List",
      ),
    )

    mkCode(
      generateHandlerDecl(block, List),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(block.projectUsesAuth) { generateExtractAuthBlock(metricSuffix) },
          parent.map { parent =>
            mkCode.doubleLines(
              generateExtractParentIDBlock(parent.decapitalizedName, metricSuffix),
              when(parent.readable == Readable.This) {
                generateCheckAuthorizationBlock(parent, false, metricSuffix)
              },
            )
          },
          queryDAOInputBlock,
          generateInvokeBeforeHookBlock(block, List, metricSuffix),
          mkCode.lines(
            metricSuffix.map(generateMetricTimerDecl),
            queryDAOBlock,
            metricSuffix.map(_ => generateMetricTimerObservation()),
            queryDAOErrorBlock,
          ),
          generateInvokeAfterHookBlock(block, List, metricSuffix),
          instantiateResponseBlock,
          mapResponseBlock,
          genMethodCall(genMethodCall("json", "NewEncoder", "w"), "Encode", s"${block.decapitalizedName}ListResp"),
          metricSuffix.map(generateMetricSuccess),
        ),
      ),
    )
  }

}
