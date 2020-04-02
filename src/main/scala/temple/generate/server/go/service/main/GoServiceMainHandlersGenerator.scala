package temple.generate.server.go.service.main

import temple.generate.CRUD._
import temple.generate.server.{CreatedByAttribute, ServiceRoot}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.server.go.common.GoCommonGenerator._
import temple.utils.StringUtils.doubleQuote

import scala.Option.when

object GoServiceMainHandlersGenerator {

  private def generateHandlerDecl(root: ServiceRoot, operation: CRUD): String =
    s"func (env *env) ${operation.toString.toLowerCase}${root.name.capitalize}Handler(w http.ResponseWriter, r *http.Request)"

  private def generateExtractAuthBlock(): String =
    mkCode.lines(
      genDeclareAndAssign(genMethodCall("util", "ExtractAuthIDFromRequest", "r.Header"), "auth", "err"),
      genIfErr(
        mkCode.lines(
          genDeclareAndAssign(
            genMethodCall(
              "util",
              "CreateErrorJSON",
              genMethodCall(
                "fmt",
                "Sprintf",
                doubleQuote("Could not authorize request: %s"),
                genMethodCall("err", "Error"),
              ),
            ),
            "errMsg",
          ),
          genMethodCall("http", "Error", "w", "errMsg", "http.StatusUnauthorized"),
          genReturn(),
        ),
      ),
    )

  private def generateListHandler(root: ServiceRoot): String = {
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
        when(byCreator) { generateExtractAuthBlock() },
      ),
    )
  }

  private def generateCreateHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(
        ),
    )

  private def generateReadHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Read),
      CodeWrap.curly.tabbed(
        ),
    )

  private def generateUpdateHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Update),
      CodeWrap.curly.tabbed(
        ),
    )

  private def generateDeleteHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Delete),
      CodeWrap.curly.tabbed(
        ),
    )

  private[service] def generateHandlers(root: ServiceRoot, operations: Set[CRUD]): String =
    mkCode.doubleLines(
      for (operation <- operations.toSeq.sorted)
        yield operation match {
          case List   => generateListHandler(root)
          case Create => generateCreateHandler(root)
          case Read   => generateReadHandler(root)
          case Update => generateUpdateHandler(root)
          case Delete => generateDeleteHandler(root)
        },
    )
}
