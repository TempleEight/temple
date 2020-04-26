package temple.generate.server.go.service.main

import temple.ast.Metadata.Readable
import temple.generate.CRUD.Read
import temple.generate.server.AttributesRoot
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainGenerator.{generateDAOReadCall, generateDAOReadInput}
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainReadHandlerGenerator {

  private def generateDAOCallBlock(block: AttributesRoot, metricSuffix: Option[String]): String =
    mkCode.doubleLines(
      generateDAOReadInput(block),
      generateInvokeBeforeHookBlock(block, Read, metricSuffix),
      mkCode.lines(
        metricSuffix.map(generateMetricTimerDecl),
        generateDAOReadCall(block),
        metricSuffix.map(_ => generateMetricTimerObservation()),
        generateDAOCallErrorBlock(block, metricSuffix),
      ),
      generateInvokeAfterHookBlock(block, Read, metricSuffix),
    )

  /** Generate the read handler function */
  private[main] def generateReadHandler(
    block: AttributesRoot,
    responseMap: ListMap[String, String],
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { Read.toString }
    mkCode(
      generateHandlerDecl(block, Read),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(block.projectUsesAuth) { generateExtractAuthBlock(metricSuffix) },
          generateExtractIDBlock(block.decapitalizedName, metricSuffix),
          when(block.readable == Readable.This) { generateCheckAuthorizationBlock(block, metricSuffix) },
          generateDAOCallBlock(block, metricSuffix),
          generateJSONResponse(s"read${block.name}", responseMap),
          metricSuffix.map(generateMetricSuccess),
        ),
      ),
    )
  }
}
