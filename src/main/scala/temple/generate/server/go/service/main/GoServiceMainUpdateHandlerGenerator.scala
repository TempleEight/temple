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

  private def generateDAOCallBlock(root: ServiceRoot, usesMetrics: Boolean): String =
    mkCode.lines(
      when(usesMetrics) { generateMetricTimerDecl(Update.toString) },
      genDeclareAndAssign(
        genMethodCall("env.dao", s"Update${root.name}", "input"),
        root.decapitalizedName,
        "err",
      ),
      when(usesMetrics) { generateMetricTimerObservation() },
      generateDAOCallErrorBlock(root),
    )

  /** Generate the update handler function */
  private[main] def generateUpdateHandler(
    root: ServiceRoot,
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
    usesMetrics: Boolean,
  ): String =
    mkCode(
      generateHandlerDecl(root, Update),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(root.writable == Writable.This) },
          generateExtractIDBlock(root.decapitalizedName),
          when(root.writable == Writable.This) { generateCheckAuthorizationBlock(root) },
          // Only need to handle request JSONs when there are client attributes
          when(root.requestAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(root, Update, s"update${root.name}"),
              generateRequestNilCheck(root.requestAttributes),
              generateValidateStructBlock(),
              when(usesComms) { generateForeignKeyCheckBlocks(root) },
              when(clientUsesTime) { generateParseTimeBlocks(root.requestAttributes) },
            )
          },
          generateDAOInput(root),
          generateInvokeBeforeHookBlock(root, Update),
          generateDAOCallBlock(root, usesMetrics),
          generateInvokeAfterHookBlock(root, Update),
          generateJSONResponse(s"update${root.name}", responseMap),
          when(usesMetrics) { generateMetricSuccess(Update.toString) },
        ),
      ),
    )
}
