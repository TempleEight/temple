package temple.generate.server.go.service.main

import temple.ast.AttributeType.DateTimeType
import temple.ast.{Annotation, Attribute, AttributeType}
import temple.generate.CRUD._
import temple.generate.server.ServiceRoot
import temple.generate.server.go.GoHTTPStatus._
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainCreateHandlerGenerator.generateCreateHandler
import temple.generate.server.go.service.main.GoServiceMainDeleteHandlerGenerator.generateDeleteHandler
import temple.generate.server.go.service.main.GoServiceMainListHandlerGenerator.generateListHandler
import temple.generate.server.go.service.main.GoServiceMainReadHandlerGenerator.generateReadHandler
import temple.generate.server.go.service.main.GoServiceMainUpdateHandlerGenerator.generateUpdateHandler
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.{decapitalize, doubleQuote}

import scala.collection.immutable.ListMap
import scala.Option.when

object GoServiceMainHandlersGenerator {

  private def generateResponseMapFormat(attributeType: AttributeType): String =
    // Must add formatting to attributes with datetime type
    attributeType match {
      case DateTimeType => s".${genFunctionCall("Format", "time.RFC3339")}"
      case _            => ""
    }

  /** Generate a map for converting the fields of the DAO response to the JSON response */
  private def generateResponseMap(root: ServiceRoot): ListMap[String, String] =
    // Includes ID attribute and all attributes without the @server annotation
    ListMap(root.idAttribute.name.toUpperCase -> s"${root.decapitalizedName}.${root.idAttribute.name.toUpperCase}") ++
    root.attributes.collect {
      case name -> attribute if !attribute.accessAnnotation.contains(Annotation.Server) =>
        name.capitalize -> s"${root.decapitalizedName}.${name.capitalize}${generateResponseMapFormat(attribute.attributeType)}"
    }

  /** Generate a handler method declaration */
  private[main] def generateHandlerDecl(root: ServiceRoot, operation: CRUD): String =
    s"func (env *env) ${operation.toString.toLowerCase}${root.name}Handler(w http.ResponseWriter, r *http.Request)"

  /** Generate a errMsg declaration and http.Error call */
  private[main] def generateHTTPError(statusCodeEnum: GoHTTPStatus, errMsg: String, errMsgArgs: String*): String = {
    val createErrorJSONargs =
      if (errMsgArgs.nonEmpty) genMethodCall("fmt", "Sprintf", doubleQuote(errMsg), errMsgArgs)
      else doubleQuote(errMsg)

    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "util",
          "CreateErrorJSON",
          createErrorJSONargs,
        ),
        "errMsg",
      ),
      genMethodCall("http", "Error", "w", "errMsg", s"http.$statusCodeEnum"),
    )
  }

  /** Generate HTTP error and return */
  private[main] def generateHTTPErrorReturn(statusCodeEnum: GoHTTPStatus, errMsg: String, errMsgArgs: String*): String =
    mkCode.lines(
      generateHTTPError(statusCodeEnum, errMsg, errMsgArgs: _*),
      genReturn(),
    )

  /** Generate the block for extracting an AuthID from the request header */
  private[main] def generateExtractAuthBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("util", "ExtractAuthIDFromRequest", "r.Header"), "auth", "err"),
      genIfErr(
        generateHTTPErrorReturn(
          StatusUnauthorized,
          "Could not authorize request: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  /** Generate the block for decoding an incoming request JSON into a request object */
  private[main] def generateDecodeRequestBlock(typePrefix: String): String =
    mkCode.lines(
      genVar("req", s"${typePrefix}Request"),
      genAssign(genMethodCall(genMethodCall("json", "NewDecoder", "r.Body"), "Decode", "&req"), "err"),
      genIfErr(
        generateHTTPErrorReturn(StatusBadRequest, "Invalid request parameters: %s", genMethodCall("err", "Error")),
      ),
    )

  /** Generate the block for validating the request object */
  private[main] def generateValidateStructBlock(): String =
    mkCode.lines(
      genAssign(genMethodCall("valid", "ValidateStruct", "req"), "_", "err"),
      genIfErr(
        generateHTTPErrorReturn(StatusBadRequest, "Invalid request parameters: %s", genMethodCall("err", "Error")),
      ),
    )

  private def generateForeignKeyCheckBlock(root: ServiceRoot, name: String, reference: String): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "env.comm",
          s"Check$reference",
          s"*req.${name.capitalize}",
          when(root.projectUsesAuth) { genMethodCall("r.Header", "Get", doubleQuote("Authorization")) },
        ),
        s"${name}Valid",
        "err",
      ),
      genIfErr(
        generateHTTPErrorReturn(
          StatusInternalServerError,
          s"Unable to reach ${decapitalize(reference)} service: %s",
          genMethodCall("err", "Error"),
        ),
      ),
      genIf(
        s"!${name}Valid",
        generateHTTPErrorReturn(
          StatusBadRequest,
          s"Unknown $reference: %s",
          genMethodCall(s"req.${name.capitalize}", "String"),
        ),
      ),
    )

  /** Generate the block for checking foreign keys against other services */
  private[main] def generateForeignKeyCheckBlocks(
    root: ServiceRoot,
    clientAttributes: ListMap[String, Attribute],
  ): String =
    mkCode.doubleLines(
      clientAttributes.map {
        case name -> attribute =>
          attribute.attributeType match {
            case AttributeType.ForeignKey(reference) => generateForeignKeyCheckBlock(root, name, reference)
            case _                                   => ""
          }
      },
    )

  /** Generate JSON response from DAO response */
  private[main] def generateJSONResponse(typePrefix: String, responseMap: ListMap[String, String]): String =
    genMethodCall(
      genMethodCall("json", "NewEncoder", "w"),
      "Encode",
      genPopulateStruct(s"${typePrefix}Response", responseMap),
    )

  /** Generate the env handler functions */
  private[service] def generateHandlers(
    root: ServiceRoot,
    operations: Set[CRUD],
    clientAttributes: ListMap[String, Attribute],
    usesComms: Boolean,
    hasAuthBlock: Boolean,
    enumeratingByCreator: Boolean,
  ): String = {
    val responseMap = generateResponseMap(root)
    mkCode.doubleLines(
      operations.toSeq.sorted.map {
        case List   => generateListHandler(root, responseMap, enumeratingByCreator)
        case Create => generateCreateHandler(root, clientAttributes, usesComms, hasAuthBlock, responseMap)
        case Read   => generateReadHandler(root)
        case Update => generateUpdateHandler(root)
        case Delete => generateDeleteHandler(root)
      },
    )
  }
}
