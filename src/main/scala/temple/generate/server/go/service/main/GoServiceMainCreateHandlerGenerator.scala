package temple.generate.server.go.service.main

import temple.ast.Annotation.Unique
import temple.ast.Metadata.Writable
import temple.generate.CRUD.Create
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.GoHTTPStatus.{StatusForbidden, StatusInternalServerError}
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
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusInternalServerError),
          metricSuffix,
          "Could not create UUID: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  private def generateDAOInput(block: AttributesRoot): String = {
    val idCapitalized = block.idAttribute.name.toUpperCase
    // If service has auth block then an AuthID is passed in as ID, otherwise a created uuid is passed in
    val createInput =
      ListMap(idCapitalized -> (if (block.hasAuthBlock) s"auth.$idCapitalized" else "uuid")) ++
      // If the project uses auth, but this service does not have an auth block, AuthID is passed for created_by field
      when(!block.hasAuthBlock && block.projectUsesAuth && block.parentAttribute.isEmpty) {
        s"Auth$idCapitalized" -> s"auth.$idCapitalized"
      } ++
      generateDAOInputClientMap(block.storedRequestAttributes)

    genDeclareAndAssign(
      genPopulateStruct(s"dao.Create${block.name}Input", createInput),
      "input",
    )
  }

  private def generateDAOCallErrorBlock(block: AttributesRoot, metricSuffix: Option[String]): String = {
    val defaultError = generateRespondWithError(
      genHTTPEnum(StatusInternalServerError),
      metricSuffix,
      "Something went wrong: %s",
      genMethodCall("err", "Error"),
    )
    genIfErr(
      if (block.contains(Unique)) {
        genSwitchReturn(
          "err.(type)",
          ListMap(
            s"dao.ErrDuplicate${block.name}" -> generateRespondWithError(
              genHTTPEnum(StatusForbidden),
              metricSuffix,
              genMethodCall("err", "Error"),
            ),
          ),
          defaultError,
        )
      } else {
        mkCode.lines(defaultError, genReturn())
      },
    )
  }

  private def generateDAOCallBlock(block: AttributesRoot, metricSuffix: Option[String]): String =
    mkCode.lines(
      metricSuffix.map(metricSuffix => generateMetricTimerDecl(metricSuffix)),
      genDeclareAndAssign(
        genMethodCall(
          "env.dao",
          s"Create${block.name}",
          "input",
        ),
        block.decapitalizedName,
        "err",
      ),
      metricSuffix.map(_ => generateMetricTimerObservation()),
      generateDAOCallErrorBlock(block, metricSuffix),
    )

  /** Generate the create handler function */
  private[main] def generateCreateHandler(
    block: AttributesRoot,
    parent: Option[ServiceRoot],
    usesComms: Boolean,
    responseMap: ListMap[String, String],
    clientUsesTime: Boolean,
    clientUsesBase64: Boolean,
    usesMetrics: Boolean,
  ): String = {
    val metricSuffix = when(usesMetrics) { Create.toString + block.structName }
    mkCode(
      generateHandlerDecl(block, Create),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(block.projectUsesAuth) { generateExtractAuthBlock(metricSuffix) },
          parent.map { parent =>
            mkCode.doubleLines(
              generateExtractParentIDBlock(parent.decapitalizedName, metricSuffix),
              when(parent.writable == Writable.This) {
                generateCheckAuthorizationBlock(parent, blockHasAuth = false, metricSuffix)
              },
            )
          },
          // Only need to handle request JSONs when there are client attributes
          when(block.requestAttributes.nonEmpty) {
            mkCode.doubleLines(
              generateDecodeRequestBlock(block, Create, s"create${block.name}", metricSuffix),
              generateRequestNilCheck(block.requestAttributes, metricSuffix),
              generateValidateStructBlock(metricSuffix),
              when(usesComms) { generateForeignKeyCheckBlocks(block, metricSuffix) },
              when(clientUsesTime) { generateParseTimeBlocks(block.requestAttributes, metricSuffix) },
              when(clientUsesBase64) { generateParseBase64Blocks(block.requestAttributes, metricSuffix) },
            )
          },
          when(!block.hasAuthBlock) { generateNewUUIDBlock(metricSuffix) },
          generateDAOInput(block),
          generateInvokeBeforeHookBlock(block, Create, metricSuffix),
          generateDAOCallBlock(block, metricSuffix),
          generateInvokeAfterHookBlock(block, Create, metricSuffix),
          generateJSONResponse(s"create${block.name}", responseMap),
          metricSuffix.map(metricSuffix => generateMetricSuccess(metricSuffix)),
        ),
      ),
    )
  }
}
