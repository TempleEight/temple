package temple.generate.server.go.service.main

import temple.ast.AttributeType.DateTimeType
import temple.ast.{Annotation, AttributeType}
import temple.generate.CRUD._
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainCreateHandlerGenerator.generateCreateHandler
import temple.generate.server.go.service.main.GoServiceMainDeleteHandlerGenerator.generateDeleteHandler
import temple.generate.server.go.service.main.GoServiceMainListHandlerGenerator.generateListHandler
import temple.generate.server.go.service.main.GoServiceMainReadHandlerGenerator.generateReadHandler
import temple.generate.server.go.service.main.GoServiceMainUpdateHandlerGenerator.generateUpdateHandler
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.doubleQuote

import scala.collection.immutable.ListMap

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
    ListMap(root.idAttribute.name.toUpperCase -> s"${root.name}.${root.idAttribute.name.toUpperCase}") ++
    root.attributes.collect {
      case name -> attribute if !attribute.accessAnnotation.contains(Annotation.Server) =>
        name.capitalize -> s"${root.name}.${name.capitalize}${generateResponseMapFormat(attribute.attributeType)}"
    }

  /** Generate a handler method declaration */
  private[main] def generateHandlerDecl(root: ServiceRoot, operation: CRUD): String =
    s"func (env *env) ${operation.toString.toLowerCase}${root.name.capitalize}Handler(w http.ResponseWriter, r *http.Request)"

  /** Generate a errMsg declaration and http.Error call */
  private[main] def generateHTTPError(statusCodeEnum: String, errMsg: String, errMsgArgs: String*): String = {
    val createErrorJSONargs =
      if (errMsgArgs.nonEmpty) genMethodCall("fmt", "Sprintf", (doubleQuote(errMsg) +: errMsgArgs): _*)
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

  /** Generate the block for extracting an AuthID from the request header */
  private[main] def generateExtractAuthBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("util", "ExtractAuthIDFromRequest", "r.Header"), "auth", "err"),
      genIfErr(
        mkCode.lines(
          generateHTTPError("StatusUnauthorized", "Could not authorize request: %s", genMethodCall("err", "Error")),
          genReturn(),
        ),
      ),
    )

  /** Generate the env handler functions */
  private[service] def generateHandlers(root: ServiceRoot, operations: Set[CRUD]): String = {
    val responseMap = generateResponseMap(root)
    mkCode.doubleLines(
      operations.toSeq.sorted.map {
        case List   => generateListHandler(root, responseMap)
        case Create => generateCreateHandler(root)
        case Read   => generateReadHandler(root)
        case Update => generateUpdateHandler(root)
        case Delete => generateDeleteHandler(root)
      },
    )
  }
}
