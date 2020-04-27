package temple.generate.server.go.service.main

import temple.generate.CRUD.Identify
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainIdentifyHandlerGenerator {

  private def generateDAOInput(block: AttributesRoot): String =
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Identify${block.name}Input", ListMap("ID" -> s"auth.ID")),
      "input",
    )

  private def generateDAOCall(block: AttributesRoot): String =
    genDeclareAndAssign(
      genMethodCall(
        "env.dao",
        s"Identify${block.name}",
        "input",
      ),
      block.decapitalizedName,
      "err",
    )

  private def generateDAOCallBlock(block: AttributesRoot, metricSuffix: Option[String]): String =
    mkCode.doubleLines(
      generateDAOInput(block),
      generateInvokeBeforeHookBlock(block, Identify, metricSuffix),
      mkCode.lines(
        metricSuffix.map(generateMetricTimerDecl),
        generateDAOCall(block),
        metricSuffix.map(_ => generateMetricTimerObservation()),
        generateDAOCallErrorBlock(block, metricSuffix),
      ),
      generateInvokeAfterHookBlock(block, Identify, metricSuffix),
    )

  // The headers used here are defined by Kong: https://docs.konghq.com/2.0.x/proxy/#3-proxying--upstream-timeouts
  private def generateRedirectResponse(block: AttributesRoot): String = mkCode.lines(
    genDeclareAndAssign(
      genMethodCall(
        "fmt",
        "Sprintf",
        doubleQuote("http://%s:%s/api%s/%s"),
        genMethodCall("r.Header", "Get", doubleQuote("X-Forwarded-Host")),
        genMethodCall("r.Header", "Get", doubleQuote("X-Forwarded-Port")),
        "r.URL.Path",
        s"${block.decapitalizedName}.ID",
      ),
      "url",
    ),
    genMethodCall("w", "Header().Set", doubleQuote("Location"), "url"),
    genMethodCall("w", "WriteHeader", "http.StatusFound"),
  )

  /** Generate the identify handler function */
  private[main] def generateIdentifyHandler(
    block: AttributesRoot,
    parent: Option[ServiceRoot],
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { Identify.toString + block.structName }

    mkCode(
      generateHandlerDecl(block, Identify),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          generateExtractAuthBlock(metricSuffix),
          generateDAOCallBlock(block, metricSuffix),
          generateRedirectResponse(block),
          metricSuffix.map(generateMetricSuccess),
        ),
      ),
    )
  }
}
