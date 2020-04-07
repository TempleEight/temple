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
  private[main] def generateDecodeRequestBlock(typePrefix: String): String =
    mkCode.lines(
      genVar("req", s"${typePrefix}Request"),
      genAssign(genMethodCall(genMethodCall("json", "NewDecoder", "r.Body"), "Decode", "&req"), "err"),
      genIfErr(
        generateHTTPErrorReturn(StatusBadRequest, "Invalid request parameters: %s", genMethodCall("err", "Error")),
      ),
    )

  /** Generate the checking that incoming request parameters are not nil */
  private[main] def generateRequestNilCheck(root: ServiceRoot, clientAttributes: ListMap[String, Attribute]): String =
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

  /** Generate DAO call block error handling for Read, Update and Delete*/
  private[main] def generateDAOCallErrorBlock(root: ServiceRoot): String =
    genIfErr(
      genSwitchReturn(
        "err.(type)",
        ListMap(s"dao.Err${root.name}NotFound" -> generateOneLineHTTPError(StatusNotFound)),
        generateHTTPError(StatusInternalServerError, "Something went wrong: %s", genMethodCall("err", "Error")),
      ),
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
    enumeratingByCreator: Boolean,
  ): String = {
    val responseMap = generateResponseMap(root)
    mkCode.doubleLines(
      operations.toSeq.sorted.map {
        case List   => generateListHandler(root, responseMap, enumeratingByCreator)
        case Create => generateCreateHandler(root, clientAttributes, usesComms, responseMap)
        case Read   => generateReadHandler(root, responseMap)
        case Update => generateUpdateHandler(root, clientAttributes, usesComms, responseMap)
        case Delete => generateDeleteHandler(root)
      },
    )
  }
}
