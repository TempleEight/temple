package temple.generate.server.go.service.main

import temple.generate.CRUD._
import temple.generate.server.ServiceRoot
import temple.generate.utils.CodeTerm.mkCode
import temple.generate.server.go.common.GoCommonGenerator._

object GoServiceMainHandlersGenerator {

  private def generateHandlerDecl(root: ServiceRoot, operation: CRUD): String =
    s"func (env *env) ${operation.toString.toLowerCase}${root.name.capitalize}Handler(w http.ResponseWriter, r *http.Request) {}"

  private def generateListHandler(root: ServiceRoot): String =
    generateHandlerDecl(root, List)

  private def generateCreateHandler(root: ServiceRoot): String =
    generateHandlerDecl(root, Create)

  private def generateReadHandler(root: ServiceRoot): String =
    generateHandlerDecl(root, Read)

  private def generateUpdateHandler(root: ServiceRoot): String =
    generateHandlerDecl(root, Update)

  private def generateDeleteHandler(root: ServiceRoot): String =
    generateHandlerDecl(root, Delete)

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
