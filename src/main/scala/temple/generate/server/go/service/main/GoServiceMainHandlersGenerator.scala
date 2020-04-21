package temple.generate.server.go.service.main

import temple.ast.AbstractAttribute.Attribute
import temple.ast.AttributeType.{BlobType, DateTimeType, DateType, TimeType}
import temple.ast.Metadata.Readable
import temple.ast.{AbstractAttribute, AttributeType}
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.GoHTTPStatus._
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainCreateHandlerGenerator.generateCreateHandler
import temple.generate.server.go.service.main.GoServiceMainDeleteHandlerGenerator.generateDeleteHandler
import temple.generate.server.go.service.main.GoServiceMainIdentifyHandlerGenerator.generateIdentifyHandler
import temple.generate.server.go.service.main.GoServiceMainListHandlerGenerator.generateListHandler
import temple.generate.server.go.service.main.GoServiceMainReadHandlerGenerator.generateReadHandler
import temple.generate.server.go.service.main.GoServiceMainUpdateHandlerGenerator.generateUpdateHandler
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.{decapitalize, doubleQuote}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainHandlersGenerator {

  private def generateResponseMapFormat(root: AttributesRoot, name: String, attributeType: AttributeType): String = {
    val responseFieldName = s"${root.decapitalizedName}.${name.capitalize}"
    attributeType match {
      case DateType =>
        s"$responseFieldName.${genFunctionCall("Format", doubleQuote("2006-01-02"))}"
      case TimeType =>
        s"$responseFieldName.${genFunctionCall("Format", doubleQuote("15:04:05.999999999"))}"
      case DateTimeType =>
        s"$responseFieldName.${genFunctionCall("Format", "time.RFC3339")}"
      case BlobType(_) =>
        genMethodCall("base64.StdEncoding", "EncodeToString", responseFieldName)
      case _ => responseFieldName
    }
  }

  /** Generate a map for converting the fields of the DAO response to the JSON response */
  private def generateResponseMap(block: AttributesRoot): ListMap[String, String] =
    // Includes ID attribute and all attributes without the @server or @client annotation
    ListMap(block.idAttribute.name.toUpperCase -> s"${block.decapitalizedName}.${block.idAttribute.name.toUpperCase}") ++
    block.attributes.collect {
      case name -> attribute if attribute.inResponse =>
        name.capitalize -> generateResponseMapFormat(block, name, attribute.attributeType)
    }

  /** Generate a handler method declaration */
  private[main] def generateHandlerDecl(block: AttributesRoot, operation: CRUD): String =
    s"func (env *env) ${operation.toString.toLowerCase}${block.name}Handler(w http.ResponseWriter, r *http.Request)"

  /** Generate a respond with error call */
  private[main] def generateRespondWithError(
    statusCode: String,
    metricSuffix: Option[String],
    errMsg: String,
    errMsgArgs: String*,
  ): String = {
    val errMsgString =
      if (errMsgArgs.nonEmpty) genMethodCall("fmt", "Sprintf", doubleQuote(errMsg), errMsgArgs)
      else errMsg
    genFunctionCall(
      "respondWithError",
      "w",
      errMsgString,
      statusCode,
      metricSuffix.map(suffix => s"metric.Request$suffix"),
    )
  }

  /** Generate respond with error call and return */
  private[main] def generateRespondWithErrorReturn(
    statusCode: String,
    metricSuffix: Option[String],
    errMsg: String,
    errMsgArgs: String*,
  ): String =
    mkCode.lines(
      generateRespondWithError(statusCode, metricSuffix, errMsg, errMsgArgs: _*),
      genReturn(),
    )

  /** Generate the block for extracting an AuthID from the request header */
  private[main] def generateExtractAuthBlock(metricSuffix: Option[String]): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("util", "ExtractAuthIDFromRequest", "r.Header"),
        "auth",
        "err",
      ),
      genIfErr(
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusUnauthorized),
          metricSuffix,
          "Could not authorize request: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  /** Generate the block for extracting ID from the request URL */
  private[main] def generateExtractIDBlock(varPrefix: String, metricSuffix: Option[String]): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("util", "ExtractIDFromRequest", genMethodCall("mux", "Vars", "r")),
        s"${varPrefix}ID",
        "err",
      ),
      genIfErr(
        generateRespondWithErrorReturn(genHTTPEnum(StatusBadRequest), metricSuffix, genMethodCall("err", "Error")),
      ),
    )

  /** Generate the block for extracting parent ID from the request URL */
  private[main] def generateExtractParentIDBlock(varPrefix: String, metricSuffix: Option[String]): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("util", "ExtractParentIDFromRequest", genMethodCall("mux", "Vars", "r")),
        s"${varPrefix}ID",
        "err",
      ),
      genIfErr(
        generateRespondWithErrorReturn(genHTTPEnum(StatusBadRequest), metricSuffix, genMethodCall("err", "Error")),
      ),
    )

  /** Generate the block for checking if a block’s parent is correct */
  private[main] def generateCheckParentBlock(
    block: AttributesRoot,
    parent: ServiceRoot,
    metricSuffix: Option[String],
  ): String =
    mkCode.lines(
      genDeclareAndAssign(
        genFunctionCall(
          s"check${block.name}Parent",
          "env",
          s"${block.decapitalizedName}ID",
          s"${parent.decapitalizedName}ID",
        ),
        "correctParent",
        "err",
      ),
      genIfErr(
        genSwitchReturn(
          "err.(type)",
          ListMap(
            s"dao.Err${block.name}NotFound" -> generateRespondWithError(
              genHTTPEnum(StatusNotFound),
              metricSuffix,
              doubleQuote("Not Found"),
            ),
          ),
          generateRespondWithError(
            genHTTPEnum(StatusInternalServerError),
            metricSuffix,
            "Something went wrong: %s",
            genMethodCall("err", "Error"),
          ),
        ),
      ),
      genIf(
        "!correctParent",
        generateRespondWithErrorReturn(genHTTPEnum(StatusNotFound), metricSuffix, doubleQuote("Not Found")),
      ),
    )

  /** Generate the block for checking if a request is authorized to perform operation  */
  private[main] def generateCheckAuthorizationBlock(
    block: AttributesRoot,
    blockHasAuth: Boolean,
    metricSuffix: Option[String],
  ): String =
    // If the service has an auth block, we can simply check the AuthID is the same as the resource ID being requested
    if (blockHasAuth) {
      mkCode.lines(
        genIf(
          s"auth.ID != ${block.decapitalizedName}ID",
          generateRespondWithErrorReturn(genHTTPEnum(StatusUnauthorized), metricSuffix, doubleQuote("Unauthorized")),
        ),
      )
    } else {
      mkCode.lines(
        genDeclareAndAssign(
          genFunctionCall("checkAuthorization", "env", s"${block.decapitalizedName}ID", "auth"),
          "authorized",
          "err",
        ),
        genIfErr(
          genSwitchReturn(
            "err.(type)",
            ListMap(
              s"dao.Err${block.name}NotFound" -> generateRespondWithError(
                genHTTPEnum(StatusUnauthorized),
                metricSuffix,
                doubleQuote("Unauthorized"),
              ),
            ),
            generateRespondWithError(
              genHTTPEnum(StatusInternalServerError),
              metricSuffix,
              "Something went wrong: %s",
              genMethodCall("err", "Error"),
            ),
          ),
        ),
        genIf(
          "!authorized",
          generateRespondWithErrorReturn(genHTTPEnum(StatusUnauthorized), metricSuffix, doubleQuote("Unauthorized")),
        ),
      )
    }

  /** Generate the block for decoding an incoming request JSON into a request object */

  private[main] def generateDecodeRequestBlock(
    block: AttributesRoot,
    op: CRUD,
    typePrefix: String,
    metricSuffix: Option[String],
  ): String = {
    val jsonDecodeCall = genMethodCall(genMethodCall("json", "NewDecoder", "r.Body"), "Decode", "&req")
    mkCode.lines(
      genVar("req", s"${typePrefix}Request"),
      if (!block.projectUsesAuth && op == Create) genDeclareAndAssign(jsonDecodeCall, "err")
      else {
        genAssign(jsonDecodeCall, "err")
      },
      genIfErr(
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusBadRequest),
          metricSuffix,
          "Invalid request parameters: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )
  }

  /** Generate the checking that incoming request parameters are not nil */
  private[main] def generateRequestNilCheck(
    clientAttributes: ListMap[String, AbstractAttribute],
    metricSuffix: Option[String],
  ): String =
    genIf(
      clientAttributes.map { case name -> _ => s"req.${name.capitalize} == nil" }.mkString(" || "),
      generateRespondWithErrorReturn(
        genHTTPEnum(StatusBadRequest),
        metricSuffix,
        doubleQuote("Missing request parameter(s)"),
      ),
    )

  /** Generate the block for validating the request object */
  private[main] def generateValidateStructBlock(metricSuffix: Option[String]): String =
    mkCode.lines(
      genAssign(genMethodCall("env.valid", "Struct", "req"), "err"),
      genIfErr(
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusBadRequest),
          metricSuffix,
          "Invalid request parameters: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  private def generateForeignKeyCheckBlock(
    block: AttributesRoot,
    name: String,
    reference: String,
    metricSuffix: Option[String],
  ): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "env.comm",
          s"Check$reference",
          s"*req.${name.capitalize}",
          when(block.projectUsesAuth) { genMethodCall("r.Header", "Get", doubleQuote("Authorization")) },
        ),
        s"${name}Valid",
        "err",
      ),
      genIfErr(
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusInternalServerError),
          metricSuffix,
          s"Unable to reach ${decapitalize(reference)} service: %s",
          genMethodCall("err", "Error"),
        ),
      ),
      genIf(
        s"!${name}Valid",
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusBadRequest),
          metricSuffix,
          s"Unknown $reference: %s",
          genMethodCall(s"req.${name.capitalize}", "String"),
        ),
      ),
    )

  /** Generate the blocks for checking foreign keys against other services */
  private[main] def generateForeignKeyCheckBlocks(block: AttributesRoot, metricSuffix: Option[String]): String =
    mkCode.doubleLines(
      block.requestAttributes.map {
        case name -> attribute =>
          attribute.attributeType match {

            case AttributeType.ForeignKey(reference) =>
              generateForeignKeyCheckBlock(block, name, reference, metricSuffix)
            case _ => ""

          }
      },
    )

  private def generateParseTimeBlock(name: String, attributeType: AttributeType, metricSuffix: Option[String]): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall(
          "time",
          "Parse",
          attributeType match {
            case DateType     => doubleQuote("2006-01-02")
            case TimeType     => doubleQuote("15:04:05.999999999")
            case DateTimeType => "time.RFC3339Nano"
            case _            => ""
          },
          s"*req.${name.capitalize}",
        ),
        name,
        "err",
      ),
      genIfErr(
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusBadRequest),
          metricSuffix,
          s"Invalid ${attributeType match {
            case DateType     => "date"
            case TimeType     => "time"
            case DateTimeType => "datetime"
            case _            => ""
          }} string: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  /** Generate the blocks for parsing attributes of type date, time or datetime */
  private[main] def generateParseTimeBlocks(
    clientAttributes: ListMap[String, AbstractAttribute],
    metricSuffix: Option[String],
  ): String =
    mkCode.doubleLines(
      clientAttributes.collect {
        case name -> attr
            if attr.attributeType == DateType || attr.attributeType == TimeType || attr.attributeType == DateTimeType =>
          generateParseTimeBlock(name, attr.attributeType, metricSuffix)
      },
    )

  private def generateParseBase64Block(
    name: String,
    metricSuffix: Option[String],
  ): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("base64.StdEncoding", "DecodeString", s"*req.${name.capitalize}"),
        name,
        "err",
      ),
      genIfErr(
        generateRespondWithErrorReturn(
          genHTTPEnum(StatusBadRequest),
          metricSuffix,
          "Invalid request parameters: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  private[main] def generateParseBase64Blocks(
    clientAttributes: ListMap[String, AbstractAttribute],
    metricSuffix: Option[String],
  ): String =
    mkCode.doubleLines(
      clientAttributes.collect {
        case name -> Attribute(BlobType(_), _, _) => generateParseBase64Block(name, metricSuffix)
      },
    )

  /** Generate a map of the client attributes to pass into the Create or Update DAO input struct */
  private[main] def generateDAOInputClientMap(
    clientAttributes: ListMap[String, AbstractAttribute],
  ): ListMap[String, String] =
    clientAttributes.map {
      case (str, attr) =>
        (
          str.capitalize,
          attr.attributeType match {
            case DateType | TimeType | DateTimeType | BlobType(_) => str
            case _                                                => s"*req.${str.capitalize}"
          },
        )
    }

  /** Generate DAO call block error handling for Read, Update and Delete */
  private[main] def generateDAOCallErrorBlock(
    block: AttributesRoot,
    parent: Option[ServiceRoot],
    metricSuffix: Option[String],
  ): String = mkCode.lines(
    genIfErr(
      genSwitchReturn(
        "err.(type)",
        ListMap(
          s"dao.Err${block.name}NotFound" -> generateRespondWithError(
            genHTTPEnum(StatusNotFound),
            metricSuffix,
            genMethodCall("err", "Error"),
          ),
        ),
        generateRespondWithError(
          genHTTPEnum(StatusInternalServerError),
          metricSuffix,
          "Something went wrong: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    ),
  )

  private[main] def generateInvokeBeforeHookBlock(
    block: AttributesRoot,
    operation: CRUD,
    metricSuffix: Option[String],
  ): String = {
    val hookArguments: Seq[String] =
      (operation match {
        case List =>
          block.readable match {
            case Readable.This => Seq("env", "&input")
            case Readable.All  => Seq("env")
          }
        case Create | Update =>
          if (block.requestAttributes.isEmpty) Seq("env", "&input") else Seq("env", "req", "&input")
        case Read | Delete | Identify =>
          Seq("env", "&input")
      }) ++ when(block.projectUsesAuth) { "auth" }

    genForLoop(
      genDeclareAndAssign(s"range env.hook.before${operation.toString}${(block).structName}Hooks", "_", "hook"),
      mkCode.lines(
        genDeclareAndAssign(
          genFunctionCall("(*hook)", hookArguments),
          "err",
        ),
        genIfErr(generateRespondWithErrorReturn("err.statusCode", metricSuffix, genMethodCall("err", "Error"))),
      ),
    )
  }

  private[main] def generateInvokeAfterHookBlock(
    block: AttributesRoot,
    operation: CRUD,
    metricSuffix: Option[String],
  ): String = {
    val hookArguments =
      (operation match {
        case List =>
          Seq("env", s"${block.decapitalizedName}List")
        case Create | Read | Update | Identify =>
          Seq("env", block.decapitalizedName)
        case Delete =>
          Seq("env")
      }) ++ when(block.projectUsesAuth) { "auth" }

    genForLoop(
      genDeclareAndAssign(s"range env.hook.after${operation.toString}${(block).structName}Hooks", "_", "hook"),
      mkCode.lines(
        genDeclareAndAssign(
          genFunctionCall("(*hook)", hookArguments),
          "err",
        ),
        genIfErr(generateRespondWithErrorReturn("err.statusCode", metricSuffix, genMethodCall("err", "Error"))),
      ),
    )
  }

  /** Generate JSON response from DAO response */
  private[main] def generateJSONResponse(typePrefix: String, responseMap: ListMap[String, String]): String =
    genMethodCall(
      genMethodCall("json", "NewEncoder", "w"),
      "Encode",
      genPopulateStruct(s"${typePrefix}Response", responseMap),
    )

  /** Generate the env handler functions */
  private[service] def generateHandlers(
    block: AttributesRoot,
    parent: Option[ServiceRoot],
    usesComms: Boolean,
    usesMetrics: Boolean,
  ): String = {
    val responseMap = generateResponseMap(block)

    // Whether or not the client attributes contain attributes of type date, time or datetime
    val clientUsesTime = Set[AttributeType](AttributeType.DateType, AttributeType.TimeType, AttributeType.DateTimeType)
      .intersect(block.requestAttributes.values.map(_.attributeType).toSet)
      .nonEmpty

    // Whether or not the client attributes contain attributes of type blob
    val clientUsesBase64 = block.requestAttributes.values.exists(_.attributeType.isInstanceOf[AttributeType.BlobType])

    mkCode.doubleLines(
      block.operations.toSeq.map {
        case List =>
          generateListHandler(block, parent, responseMap, usesMetrics)
        case Create =>
          generateCreateHandler(block, parent, usesComms, responseMap, clientUsesTime, clientUsesBase64, usesMetrics)
        case Read =>
          generateReadHandler(block, parent, responseMap, usesMetrics)
        case Update =>
          generateUpdateHandler(block, parent, usesComms, responseMap, clientUsesTime, clientUsesBase64, usesMetrics)
        case Delete =>
          generateDeleteHandler(block, parent, usesMetrics)
        case Identify =>
          generateIdentifyHandler(block, parent, usesMetrics)
      },
    )
  }
}
