package temple.generate.server.go.service.main

import temple.generate.CRUD.Update
import temple.generate.server.ServiceRoot
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.generateHandlerDecl
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceMainUpdateHandlerGenerator {

  /** Generate the update handler function */
  private[main] def generateUpdateHandler(root: ServiceRoot): String =
    mkCode(
      generateHandlerDecl(root, Update),
      CodeWrap.curly.tabbed(),
    )
}
