package temple.generate.server.go.service.main

import temple.ast.AbstractAttribute
import temple.ast.Metadata.Writable
import temple.generate.CRUD.Update
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainUpdateHandlerGenerator {

  private def generateDAOInput(root: ServiceRoot, clientAttributes: ListMap[String, AbstractAttribute]): String = {
    val updateInput = ListMap("ID" -> s"${root.decapitalizedName}ID") ++ generateDAOInputClientMap(clientAttributes)
    genDeclareAndAssign(
      genPopulateStruct(s"dao.Update${root.name}Input", updateInput),
      "input",
    )
  }

  private def generateDAOCallBlock(root: ServiceRoot, usesMetrics: Boolean, metricSuffix: Option[String]): String =
    mkCode.lines(
      when(usesMetrics) { generateMetricTimerDecl(Update.toString) },
      genDeclareAndAssign(
        genMethodCall("env.dao", s"Update${root.name}", "input"),
        root.decapitalizedName,
        "err",
      ),
      when(usesMetrics) { generateMetricTimerObservation() },
      generateDAOCallErrorBlock(root, metricSuffix),
    )

  /** Generate the update handler function */
  private[main] def generateUpdateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { Update.toString }
    mkCode(
      generateHandlerDecl(root, Update),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(root.writable == Writable.This, metricSuffix) },
          generateExtractIDBlock(root.decapitalizedName, metricSuffix),
          when(root.writable == Writable.This) { generateCheckAuthorizationBlock(root, metricSuffix) },
          // Only need to handle request JSONs when there are client attributes
          when(clientAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(root, Update, s"update${root.name}", metricSuffix),
              generateRequestNilCheck(root, clientAttributes, metricSuffix),
              generateValidateStructBlock(metricSuffix),
              when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes, metricSuffix) },
              when(clientUsesTime) { generateParseTimeBlocks(clientAttributes, metricSuffix) },
            )
          },
          generateDAOInput(root, clientAttributes),
          generateInvokeBeforeHookBlock(root, clientAttributes, Update, metricSuffix),
          generateDAOCallBlock(root, usesMetrics, metricSuffix),
          generateInvokeAfterHookBlock(root, Update, metricSuffix),
          generateJSONResponse(s"update${root.name}", responseMap),
          when(usesMetrics) { generateMetricSuccess(Update.toString) },
        ),
      ),
    )
  }
}
