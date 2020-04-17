package temple.generate.server.go.service.main

import temple.ast.Metadata.Writable
import temple.generate.CRUD.Update
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainUpdateHandlerGenerator {

  private def generateDAOInput(root: ServiceRoot): String = {
    val updateInput =
      ListMap("ID" -> s"${root.decapitalizedName}ID") ++
      generateDAOInputClientMap(root.requestAttributes)

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
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
    clientUsesBase64: Boolean,
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
          when(root.requestAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(root, Update, s"update${root.name}", metricSuffix),
              generateRequestNilCheck(root.requestAttributes, metricSuffix),
              generateValidateStructBlock(metricSuffix),
              when(usesComms) { generateForeignKeyCheckBlocks(root, metricSuffix) },
              when(clientUsesTime) { generateParseTimeBlocks(root.requestAttributes, metricSuffix) },
              when(clientUsesBase64) { generateParseBase64Blocks(root.requestAttributes, metricSuffix) },
            )
          },
          generateDAOInput(root),
          generateInvokeBeforeHookBlock(root, Update, metricSuffix),
          generateDAOCallBlock(root, usesMetrics, metricSuffix),
          generateInvokeAfterHookBlock(root, Update, metricSuffix),
          generateJSONResponse(s"update${root.name}", responseMap),
          when(usesMetrics) { generateMetricSuccess(Update.toString) },
        ),
      ),
    )
  }
}
