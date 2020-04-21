package temple.generate.server.go.service.main

import temple.ast.Metadata.Writable
import temple.generate.CRUD.Update
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainUpdateHandlerGenerator {

  private def generateDAOInput(block: AttributesRoot): String = {
    val updateInput =
      ListMap("ID" -> s"${block.decapitalizedName}ID") ++
      generateDAOInputClientMap(block.requestAttributes)

    genDeclareAndAssign(
      genPopulateStruct(s"dao.Update${block.name}Input", updateInput),
      "input",
    )
  }

  private def generateDAOCallBlock(
    block: AttributesRoot,
    parent: Option[ServiceRoot],
    metricSuffix: Option[String],
  ): String =
    mkCode.lines(
      metricSuffix.map(metricSuffix => generateMetricTimerDecl(metricSuffix)),
      genDeclareAndAssign(
        genMethodCall("env.dao", s"Update${block.name}", "input"),
        block.decapitalizedName,
        "err",
      ),
      metricSuffix.map(_ => generateMetricTimerObservation()),
      generateDAOCallErrorBlock(block, parent, metricSuffix),
    )

  /** Generate the update handler function */
  private[main] def generateUpdateHandler(
    block: AttributesRoot,
    parent: Option[ServiceRoot],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
    clientUsesBase64: Boolean,
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { Update.toString + block.name }
    mkCode(
      generateHandlerDecl(block, Update),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(block.projectUsesAuth) { generateExtractAuthBlock(metricSuffix) },
          generateExtractIDBlock(block.decapitalizedName, metricSuffix),
          parent.map(parent =>
            Seq(
              generateExtractParentIDBlock(parent.decapitalizedName, metricSuffix),
              generateCheckParentBlock(block, parent, metricSuffix),
            ),
          ),
          when(block.writable == Writable.This) {
            generateCheckAuthorizationBlock(parent getOrElse block, block.hasAuthBlock, metricSuffix)
          },
          // Only need to handle request JSONs when there are client attributes
          when(block.requestAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(block, Update, s"update${block.name}", metricSuffix),
              generateRequestNilCheck(block.requestAttributes, metricSuffix),
              generateValidateStructBlock(metricSuffix),
              when(usesComms) { generateForeignKeyCheckBlocks(block, metricSuffix) },
              when(clientUsesTime) { generateParseTimeBlocks(block.requestAttributes, metricSuffix) },
              when(clientUsesBase64) { generateParseBase64Blocks(block.requestAttributes, metricSuffix) },
            )
          },
          generateDAOInput(block),
          generateInvokeBeforeHookBlock(block, Update, metricSuffix),
          generateDAOCallBlock(block, parent, metricSuffix),
          generateInvokeAfterHookBlock(block, Update, metricSuffix),
          generateJSONResponse(s"update${block.name}", responseMap),
          metricSuffix.map(metricSuffix => generateMetricSuccess(metricSuffix)),
        ),
      ),
    )
  }
}
