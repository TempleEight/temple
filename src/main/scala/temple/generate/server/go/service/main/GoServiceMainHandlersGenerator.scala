package temple.generate.server.go.service.main

import temple.ast.AttributeType.DateTimeType
import temple.ast.{Annotation, AttributeType}
import temple.generate.CRUD._
import temple.generate.server.{CreatedByAttribute, ServiceRoot}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.server.go.common.GoCommonGenerator._
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainHandlersGenerator {

  /** Generate a map for converting the fields of the DAO response to the JSON response  */
  private def generateResponseMap(root: ServiceRoot): ListMap[String, String] =
    // Includes ID attribute and all attributes without the @server annotation
    ListMap(root.idAttribute.name.toUpperCase -> s"${root.name}.${root.idAttribute.name.toUpperCase}") ++
    root.attributes.collect {
      case (name, attribute) if !attribute.accessAnnotation.contains(Annotation.Server) =>
        (
          name.capitalize,
          s"${root.name}.${name.capitalize}" +
          // Must add formatting to attributes with datetime type
          (attribute.attributeType match {
            case DateTimeType => s".${genFunctionCall("Format", "time.RFC3339")}"
            case _            => ""
          }),
        )
    }

  /** Generate a handler method declaration */
  private def generateHandlerDecl(root: ServiceRoot, operation: CRUD): String =
    s"func (env *env) ${operation.toString.toLowerCase}${root.name.capitalize}Handler(w http.ResponseWriter, r *http.Request)"

  /** Generate a errMsg declaration and http.Error call */
  private def generateHTTPError(statusCodeEnum: String, errMsg: String, errMsgArgs: String*): String = {
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
  private def generateExtractAuthBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("util", "ExtractAuthIDFromRequest", "r.Header"), "auth", "err"),
      genIfErr(
        mkCode.lines(
          generateHTTPError("StatusUnauthorized", "Could not authorize request: %s", genMethodCall("err", "Error")),
          genReturn(),
        ),
      ),
    )

  /** Generate the list handler function */
  private def generateListHandler(root: ServiceRoot, responseMap: ListMap[String, String]): String = {
    // Whether enumerating by created_by field or not
    val byCreator = root.createdByAttribute match {
      case CreatedByAttribute.None => false
      case enumerating: CreatedByAttribute.Enumerating =>
        enumerating match {
          case _: CreatedByAttribute.EnumerateByCreator => true
          case _: CreatedByAttribute.EnumerateByAll     => false
        }
    }

    mkCode(
      generateHandlerDecl(root, List),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(byCreator) { generateExtractAuthBlock() },
          mkCode.lines(
            genDeclareAndAssign(
              genMethodCall(
                "env.dao",
                s"List${root.name.capitalize}",
                when(byCreator) {
                  genPopulateStruct(
                    s"dao.List${root.name.capitalize}Input",
                    ListMap(s"AuthID" -> "auth.ID"),
                  )
                },
              ),
              s"${root.name}List",
              "err",
            ),
            genIfErr(
              mkCode.lines(
                generateHTTPError(
                  "StatusInternalServerError",
                  "Something went wrong: %s",
                  genMethodCall("err", "Error"),
                ),
                genReturn(),
              ),
            ),
          ),
          genDeclareAndAssign(
            genPopulateStruct(
              s"list${root.name.capitalize}Response",
              ListMap(
                s"${root.name.capitalize}List" -> genFunctionCall("make", s"[]list${root.name.capitalize}Element", "0"),
              ),
            ),
            s"${root.name}ListResp",
          ),
          genForLoop(
            genDeclareAndAssign(s"range *${root.name}List", "_", root.name),
            genAssign(
              genFunctionCall(
                "append",
                s"${root.name}ListResp.${root.name.capitalize}List",
                genPopulateStruct(s"list${root.name.capitalize}Element", responseMap),
              ),
              s"${root.name}ListResp.${root.name.capitalize}List",
            ),
          ),
          genMethodCall(genMethodCall("json", "NewEncoder", "w"), "Encode", s"${root.name}ListResp"),
        ),
      ),
    )
  }

  /** Generate the create handler function */
  private def generateCreateHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(
        ),
    )

  /** Generate the read handler function */
  private def generateReadHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Read),
      CodeWrap.curly.tabbed(
        ),
    )

  /** Generate the update handler function */
  private def generateUpdateHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Update),
      CodeWrap.curly.tabbed(
        ),
    )

  /** Generate the delete handler function */
  private def generateDeleteHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Delete),
      CodeWrap.curly.tabbed(
        ),
    )

  /** Generate the env handler functions */
  private[service] def generateHandlers(root: ServiceRoot, operations: Set[CRUD]): String = {
    val responseMap = generateResponseMap(root)
    mkCode.doubleLines(
      for (operation <- operations.toSeq.sorted)
        yield operation match {
          case List   => generateListHandler(root, responseMap)
          case Create => generateCreateHandler(root)
          case Read   => generateReadHandler(root)
          case Update => generateUpdateHandler(root)
          case Delete => generateDeleteHandler(root)
        },
    )
  }
}
