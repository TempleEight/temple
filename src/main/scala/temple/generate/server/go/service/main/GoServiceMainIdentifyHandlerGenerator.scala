package temple.generate.server.go.service.main

import temple.generate.CRUD.Identify
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import Option.when
import scala.collection.immutable.ListMap

object GoServiceMainIdentifyHandlerGenerator {

  private def generateDAOInput(root: ServiceRoot): String =
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Identify${root.name}Input", ListMap("ID" -> s"auth.ID")),
      "input",
    )

  private def generateDAOCall(root: ServiceRoot): String =
    genDeclareAndAssign(
      genMethodCall(
        "env.dao",
        s"Identify${root.name}",
        "input",
      ),
      root.decapitalizedName,
      "err",
    )

  private def generateDAOCallBlock(root: ServiceRoot, usesMetrics: Boolean, metricSuffix: Option[String]): String =
    mkCode.doubleLines(
      generateDAOInput(root),
      generateInvokeBeforeHookBlock(root, Identify, metricSuffix),
      mkCode.lines(
        when(usesMetrics) { generateMetricTimerDecl(Identify.toString) },
        generateDAOCall(root),
        when(usesMetrics) { generateMetricTimerObservation() },
        generateDAOCallErrorBlock(root, metricSuffix),
      ),
      generateInvokeAfterHookBlock(root, Identify, metricSuffix),
    )

  // The headers used here are defined by Kong: https://docs.konghq.com/2.0.x/proxy/#3-proxying--upstream-timeouts
  private def generateRedirectResponse(root: ServiceRoot): String = mkCode.lines(
    genDeclareAndAssign(
      genMethodCall(
        "fmt",
        "Sprintf",
        doubleQuote("%s:%s/api%s/%s"),
        genMethodCall("r.Header", "Get", doubleQuote("X-Forwarded-Host")),
        genMethodCall("r.Header", "Get", doubleQuote("X-Forwarded-Port")),
        "r.URL.Path",
        s"${root.decapitalizedName}.ID",
      ),
      "url",
    ),
    genMethodCall("w", "Header().Set", doubleQuote("Location"), "url"),
    genMethodCall("w", "WriteHeader", "http.StatusFound"),
  )

  /** Generate the identify handler function */
  private[main] def generateIdentifyHandler(root: ServiceRoot, usesMetrics: Boolean): String = {
    val metricSuffix = when(usesMetrics) { Identify.toString }

    mkCode(
      generateHandlerDecl(root, Identify),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          generateExtractAuthBlock(usesVar = true, metricSuffix),
          generateDAOCallBlock(root, usesMetrics, metricSuffix),
          generateRedirectResponse(root),
          when(usesMetrics) { generateMetricSuccess(Identify.toString) },
        ),
      ),
    )
  }
}
