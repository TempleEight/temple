package temple.generate.server.go.service.main

import temple.generate.CRUD.Create
import temple.generate.server.ServiceRoot
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.generateHandlerDecl
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceMainCreateHandlerGenerator {

  /** Generate the create handler function */
  private[main] def generateCreateHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(),
    )
}
