package temple.generate.server.go.service.main

import temple.ast.Metadata.Writable
import temple.generate.CRUD.Delete
import temple.generate.server.AttributesRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainDeleteHandlerGenerator {

  private def generateDAOInput(block: AttributesRoot): String =
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Delete${block.name}Input", ListMap("ID" -> s"${block.decapitalizedName}ID")),
      "input",
    )

  private def generateDAOCallBlock(block: AttributesRoot, metricSuffix: Option[String]): String =
    mkCode.lines(
      metricSuffix.map(generateMetricTimerDecl),
      genAssign(
        genMethodCall(
          "env.dao",
          s"Delete${block.name}",
          "input",
        ),
        "err",
      ),
      metricSuffix.map(_ => generateMetricTimerObservation()),
      generateDAOCallErrorBlock(block, metricSuffix),
    )

  /** Generate the delete handler function */
  private[main] def generateDeleteHandler(block: AttributesRoot, usesMetrics: Boolean): String = {
    val metricSuffix = when(usesMetrics) { Delete.toString }
    mkCode(
      generateHandlerDecl(block, Delete),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(block.projectUsesAuth) { generateExtractAuthBlock(metricSuffix) },
          generateExtractIDBlock(block.decapitalizedName, metricSuffix),
          when(block.writable == Writable.This) { generateCheckAuthorizationBlock(block, metricSuffix) },
          generateDAOInput(block),
          generateInvokeBeforeHookBlock(block, Delete, metricSuffix),
          generateDAOCallBlock(block, metricSuffix),
          generateInvokeAfterHookBlock(block, Delete, metricSuffix),
          genMethodCall(genMethodCall("json", "NewEncoder", "w"), "Encode", "struct{}{}"),
          metricSuffix.map(metricSuffix => generateMetricSuccess(metricSuffix)),
        ),
      ),
    )
  }
}
