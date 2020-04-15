package temple.generate.server.go.service.main

import temple.generate.CRUD.Identify
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

object GoServiceMainIdentityHandlerGenerator {

  /** Generate the identity handler function */
  private[main] def generateIdentityHandler(root: ServiceRoot, usesMetrics: Boolean): String = mkCode(
    generateHandlerDecl(root, Identify),
    CodeWrap.curly.tabbed(
      mkCode.doubleLines(
        // TODO!
      ),
    ),
  )
}
