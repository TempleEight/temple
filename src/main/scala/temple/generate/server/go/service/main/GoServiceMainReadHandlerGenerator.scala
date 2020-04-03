package temple.generate.server.go.service.main

import temple.generate.CRUD.Read
import temple.generate.server.ServiceRoot
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.generateHandlerDecl
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceMainReadHandlerGenerator {

  /** Generate the read handler function */
  private[main] def generateReadHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Read),
      CodeWrap.curly.tabbed(),
    )
}
