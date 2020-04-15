package temple.generate.server.go.service.main

import temple.ast.AbstractAttribute
import temple.generate.CRUD.Create
import temple.generate.server.ServiceRoot
import temple.generate.server.go.GoHTTPStatus.StatusInternalServerError
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.common.GoCommonMainGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainCreateHandlerGenerator {

  /** Generate new UUID block */
  private def generateNewUUIDBlock(metricSuffix: Option[String]): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("uuid", "NewUUID"), "uuid", "err"),
      genIfErr(
        generateRespondWithErrorReturn(genHttpEnum(StatusInternalServerError), metricSuffix)(
          "Could not create UUID: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  private def generateDAOInput(root: ServiceRoot, clientAttributes: ListMap[String, AbstractAttribute]): String = {
    val idCapitalized = root.idAttribute.name.toUpperCase
    // If service has auth block then an AuthID is passed in as ID, otherwise a created uuid is passed in
    val createInput = ListMap(idCapitalized -> (if (root.hasAuthBlock) s"auth.$idCapitalized" else "uuid")) ++
      // If the project uses auth, but this service does not have an auth block, AuthID is passed for created_by field
      when(!root.hasAuthBlock && root.projectUsesAuth) { s"Auth$idCapitalized" -> s"auth.$idCapitalized" } ++
      generateDAOInputClientMap(clientAttributes)

    genDeclareAndAssign(
      genPopulateStruct(s"dao.Create${root.name}Input", createInput),
      "input",
    )
  }

  private def generateDAOCallBlock(root: ServiceRoot, usesMetrics: Boolean, metricSuffix: Option[String]): String =
    mkCode.lines(
      when(usesMetrics) { generateMetricTimerDecl(Create.toString) },
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"Create${root.name}",
          "input",
        ),
        root.decapitalizedName,
        "err",
      ),
      when(usesMetrics) { generateMetricTimerObservation() },
      genIfErr(
        generateRespondWithErrorReturn(genHttpEnum(StatusInternalServerError), metricSuffix)(
          "Something went wrong: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  /** Generate the create handler function */
  private[main] def generateCreateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { Create.toString }
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(usesVar = true, metricSuffix) },
          // Only need to handle request JSONs when there are client attributes
          when(clientAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(root, Create, s"create${root.name}", metricSuffix),
              generateRequestNilCheck(root, clientAttributes, metricSuffix),
              generateValidateStructBlock(metricSuffix),
              when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes, metricSuffix) },
              when(clientUsesTime) { generateParseTimeBlocks(clientAttributes, metricSuffix) },
            )
          },
          when(!root.hasAuthBlock) { generateNewUUIDBlock(metricSuffix) },
          generateDAOInput(root, clientAttributes),
          generateInvokeBeforeHookBlock(root, clientAttributes, Create, metricSuffix),
          generateDAOCallBlock(root, usesMetrics, metricSuffix),
          generateInvokeAfterHookBlock(root, Create, metricSuffix),
          generateJSONResponse(s"create${root.name}", responseMap),
          when(usesMetrics) { generateMetricSuccess(Create.toString) },
        ),
      ),
    )
  }
}
