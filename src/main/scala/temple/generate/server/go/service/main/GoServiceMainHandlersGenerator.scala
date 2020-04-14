package temple.generate.server.go.service.main

import temple.ast.AttributeType.{BlobType, DateTimeType, DateType, TimeType}
import temple.ast.Metadata.Readable
import temple.ast.{AbstractAttribute, Annotation, AttributeType}
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

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainHandlersGenerator {

  private def generateResponseMapFormat(root: ServiceRoot, name: String, attributeType: AttributeType): String = {
    val responseFieldName = s"${root.decapitalizedName}.${name.capitalize}"
    attributeType match {
      case DateType =>
        s"$responseFieldName.${genFunctionCall("Format", doubleQuote("2006-01-02"))}"
      case TimeType =>
        s"$responseFieldName.${genFunctionCall("Format", doubleQuote("15:04:05.999999999"))}"
      case DateTimeType =>
        s"$responseFieldName.${genFunctionCall("Format", "time.RFC3339")}"
      case _: BlobType =>
        genMethodCall("base64.StdEncoding", "EncodeToString", responseFieldName)
      case _ => responseFieldName
    }
  }

  /** Generate a map for converting the fields of the DAO response to the JSON response */
  private def generateResponseMap(root: ServiceRoot): ListMap[String, String] =
    // Includes ID attribute and all attributes without the @server annotation
    ListMap(root.idAttribute.name.toUpperCase -> s"${root.decapitalizedName}.${root.idAttribute.name.toUpperCase}") ++
    root.attributes.collect {
      case name -> attribute if attribute.inResponse =>
        name.capitalize -> generateResponseMapFormat(root, name, attribute.attributeType)
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

  /** Generate one line http.Error call */
  private[main] def generateOneLineHTTPError(statusCodeEnum: GoHTTPStatus): String =
    genMethodCall(
      "http",
      "Error",
      "w",
      genMethodCall("util", "CreateErrorJSON", genMethodCall("err", "Error")),
      s"http.$statusCodeEnum",
    )

  /** Generate one line http.Error call and return */
  private[main] def generateOneLineHTTPErrorReturn(statusCodeEnum: GoHTTPStatus): String =
    mkCode.lines(
      generateOneLineHTTPError(statusCodeEnum),
      genReturn(),
    )

  /**
    * Generate the block for extracting an AuthID from the request header
    *
    * @param usesVar indicates whether to assign the auth to a variable, i.e. "auth" | "_"
    * @return extract auth block string
    */
  private[main] def generateExtractAuthBlock(usesVar: Boolean): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("util", "ExtractAuthIDFromRequest", "r.Header"),
        if (usesVar) "auth" else "_",
        "err",
      ),
      genIfErr(
        generateHTTPErrorReturn(
          StatusUnauthorized,
          "Could not authorize request: %s",
          genMethodCall("err", "Error"),
        ),
      ),
    )

  /** Generate the block for extracting and ID from the request URL */
  private[main] def generateExtractIDBlock(varPrefix: String): String =
    mkCode.lines(
      genDeclareAndAssign(
        genMethodCall("util", "ExtractIDFromRequest", genMethodCall("mux", "Vars", "r")),
        s"${varPrefix}ID",
        "err",
      ),
      genIfErr(generateOneLineHTTPErrorReturn(StatusBadRequest)),
    )

  /** Generate the block for checking if a request is authorized to perform operation  */
  private[main] def generateCheckAuthorizationBlock(root: ServiceRoot): String =
    // If the service has an auth block, we can simply check the AuthID is the same as the resource ID being requested
    if (root.hasAuthBlock) {
      mkCode.lines(
        genIf(s"auth.ID != ${root.decapitalizedName}ID", generateHTTPErrorReturn(StatusUnauthorized, "Unauthorized")),
      )
    } else {
      mkCode.lines(
        genDeclareAndAssign(
          genFunctionCall("checkAuthorization", "env", s"${root.decapitalizedName}ID", "auth"),
          "authorized",
          "err",
        ),
        genIfErr(
          genSwitchReturn(
            "err.(type)",
            ListMap(s"dao.Err${root.name}NotFound" -> generateHTTPError(StatusUnauthorized, "Unauthorized")),
            generateHTTPError(StatusInternalServerError, "Something went wrong: %s", genMethodCall("err", "Error")),
          ),
        ),
        genIf("!authorized", generateHTTPErrorReturn(StatusUnauthorized, "Unauthorized")),
      )
    }

  /** Generate the block for decoding an incoming request JSON into a request object */
  private[main] def generateDecodeRequestBlock(root: ServiceRoot, op: CRUD, typePrefix: String): String = {
    val jsonDecodeCall = genMethodCall(genMethodCall("json", "NewDecoder", "r.Body"), "Decode", "&req")
    mkCode.lines(
      genVar("req", s"${typePrefix}Request"),
      if (!root.projectUsesAuth && op == Create) genDeclareAndAssign(jsonDecodeCall, "err")
      else {
        genAssign(jsonDecodeCall, "err")
      },
      genIfErr(
        generateHTTPErrorReturn(StatusBadRequest, "Invalid request parameters: %s", genMethodCall("err", "Error")),
      ),
    )
  }

  /** Generate the checking that incoming request parameters are not nil */
  private[main] def generateRequestNilCheck(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
  ): String =
    genIf(
      clientAttributes.map { case name -> _ => s"req.${name.capitalize} == nil" }.mkString(" || "),
      generateHTTPErrorReturn(StatusBadRequest, "Missing request parameter(s)"),
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

  /** Generate the blocks for checking foreign keys against other services */
  private[main] def generateForeignKeyCheckBlocks(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
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

  private def generateParseTimeBlock(name: String, attributeType: AttributeType): String =
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
        generateHTTPErrorReturn(
          StatusBadRequest,
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
  private[main] def generateParseTimeBlocks(clientAttributes: ListMap[String, AbstractAttribute]): String =
    mkCode.doubleLines(
      clientAttributes.collect {
        case name -> attr
            if attr.attributeType == DateType || attr.attributeType == TimeType || attr.attributeType == DateTimeType =>
          generateParseTimeBlock(name, attr.attributeType)
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
          // Date, time or datetime attributes pass a pre-parsed variable of the same name
          if (attr.attributeType == DateType || attr.attributeType == TimeType || attr.attributeType == DateTimeType) {
            str
          } else {
            s"*req.${str.capitalize}"
          },
        )
    }

  /** Generate the metric timer declaration, ready to measure the duration of a DAO call */
  private[main] def generateMetricTimerDecl(op: CRUD): String =
    genDeclareAndAssign(
      genMethodCall(
        "prometheus",
        "NewTimer",
        genMethodCall("metric.DatabaseRequestDuration", "WithLabelValues", s"metric.Request$op"),
      ),
      "timer",
    )

  /** Generate the metric timer observation, to log the duration of a DAO call */
  private[main] def generateMetricTimerObservation(): String =
    genMethodCall("timer", "ObserveDuration")

  /** Generate DAO call block error handling for Read, Update and Delete */
  private[main] def generateDAOCallErrorBlock(root: ServiceRoot): String =
    genIfErr(
      genSwitchReturn(
        "err.(type)",
        ListMap(s"dao.Err${root.name}NotFound" -> generateOneLineHTTPError(StatusNotFound)),
        generateHTTPError(StatusInternalServerError, "Something went wrong: %s", genMethodCall("err", "Error")),
      ),
    )

  private[main] def generateInvokeBeforeHookBlock(
    root: ServiceRoot,
    clientAttributes: ListMap[String, AbstractAttribute],
    operation: CRUD,
  ): String = {
    val hookArguments: Seq[String] = operation match {
      case List =>
        root.readable match {
          case Readable.This => Seq("env", "&input")
          case Readable.All  => Seq("env")
        }
      case Create | Update =>
        if (clientAttributes.isEmpty) Seq("env", "&input") else Seq("env", "req", "&input")
      case Read | Delete =>
        Seq("env", "&input")
    }

    genForLoop(
      genDeclareAndAssign(s"range env.hook.before${operation.toString}Hooks", "_", "hook"),
      mkCode.lines(
        genDeclareAndAssign(
          genFunctionCall("(*hook)", hookArguments),
          "err",
        ),
        // TODO: replace with `respondWithError` call
        genIfErr(mkCode.lines("// TODO", genReturn())),
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

  /** Generate the metric call to log a successful request */
  private[main] def generateMetricSuccess(op: CRUD): String =
    genMethodCall(genMethodCall("metric.RequestSuccess", "WithLabelValues", s"metric.Request$op"), "Inc")

  /** Generate the env handler functions */
  private[service] def generateHandlers(
    root: ServiceRoot,
    operations: Set[CRUD],
    clientAttributes: ListMap[String, AbstractAttribute],
    usesComms: Boolean,
    enumeratingByCreator: Boolean,
    usesMetrics: Boolean,
  ): String = {
    val responseMap = generateResponseMap(root)

    // Whether or not the client attributes contain attributes of type date, time or datetime
    val clientUsesTime = Set[AttributeType](AttributeType.DateType, AttributeType.TimeType, AttributeType.DateTimeType)
      .intersect(clientAttributes.values.map(_.attributeType).toSet)
      .nonEmpty

    mkCode.doubleLines(
      operations.toSeq.sorted.map {
        case List =>
          generateListHandler(root, responseMap, enumeratingByCreator, usesMetrics)
        case Create =>
          generateCreateHandler(root, clientAttributes, usesComms, responseMap, clientUsesTime, usesMetrics)
        case Read =>
          generateReadHandler(root, responseMap, usesMetrics)
        case Update =>
          generateUpdateHandler(root, clientAttributes, usesComms, responseMap, clientUsesTime, usesMetrics)
        case Delete =>
          generateDeleteHandler(root, usesMetrics)
      },
    )
  }
}
