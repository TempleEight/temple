package temple.generate.server.go.service.main

import temple.generate.CRUD.Delete
import temple.generate.server.ServiceRoot
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.generateHandlerDecl
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceMainDeleteHandlerGenerator {

  /** Generate the delete handler function */
  private[main] def generateDeleteHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Delete),
      CodeWrap.curly.tabbed(),
    )
}
