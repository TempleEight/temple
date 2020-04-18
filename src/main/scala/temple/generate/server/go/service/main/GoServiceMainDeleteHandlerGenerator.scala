package temple.generate.server.go.service.main

import temple.ast.Metadata.Writable
import temple.generate.CRUD.Delete
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainDeleteHandlerGenerator {

  private def generateDAOInput(root: ServiceRoot): String =
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Delete${root.name}Input", ListMap("ID" -> s"${root.decapitalizedName}ID")),
      "input",
    )

  private def generateDAOCallBlock(root: ServiceRoot, usesMetrics: Boolean, metricSuffix: Option[String]): String =
    mkCode.lines(
      when(usesMetrics) { generateMetricTimerDecl(Delete.toString) },
      genAssign(
        genMethodCall(
          "env.dao",
          s"Delete${root.name}",
          "input",
        ),
        "err",
      ),
      when(usesMetrics) { generateMetricTimerObservation() },
      generateDAOCallErrorBlock(root, metricSuffix),
    )

  /** Generate the delete handler function */
  private[main] def generateDeleteHandler(root: ServiceRoot, usesMetrics: Boolean): String = {
    val metricSuffix = when(usesMetrics) { Delete.toString }
    mkCode(
      generateHandlerDecl(root, Delete),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(metricSuffix) },
          generateExtractIDBlock(root.decapitalizedName, metricSuffix),
          when(root.writable == Writable.This) { generateCheckAuthorizationBlock(root, metricSuffix) },
          generateDAOInput(root),
          generateInvokeBeforeHookBlock(root, Delete, metricSuffix),
          generateDAOCallBlock(root, usesMetrics, metricSuffix),
          generateInvokeAfterHookBlock(root, Delete, metricSuffix),
          genMethodCall(genMethodCall("json", "NewEncoder", "w"), "Encode", "struct{}{}"),
          when(usesMetrics) { generateMetricSuccess(Delete.toString) },
        ),
      ),
    )
  }
}
