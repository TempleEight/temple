package temple.generate.server.go.service.main

import temple.ast.Metadata.Readable
import temple.generate.CRUD.Read
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainGenerator.{generateDAOReadCall, generateDAOReadInput}
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainReadHandlerGenerator {

  private def generateDAOCallBlock(root: ServiceRoot, usesMetrics: Boolean, metricSuffix: Option[String]): String =
    mkCode.doubleLines(
      generateDAOReadInput(root),
      generateInvokeBeforeHookBlock(root, Read, metricSuffix),
      mkCode.lines(
        when(usesMetrics) { generateMetricTimerDecl(Read.toString) },
        generateDAOReadCall(root),
        when(usesMetrics) { generateMetricTimerObservation() },
        generateDAOCallErrorBlock(root, metricSuffix),
      ),
      generateInvokeAfterHookBlock(root, Read, metricSuffix),
    )

  /** Generate the read handler function */
  private[main] def generateReadHandler(
    root: ServiceRoot,
    responseMap: ListMap[String, String],
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { Read.toString }
    mkCode(
      generateHandlerDecl(root, Read),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(metricSuffix) },
          generateExtractIDBlock(root.decapitalizedName, metricSuffix),
          when(root.readable == Readable.This) { generateCheckAuthorizationBlock(root, metricSuffix) },
          generateDAOCallBlock(root, usesMetrics, metricSuffix),
          generateJSONResponse(s"read${root.name}", responseMap),
          when(usesMetrics) { generateMetricSuccess(Read.toString) },
        ),
      ),
    )
  }
}
