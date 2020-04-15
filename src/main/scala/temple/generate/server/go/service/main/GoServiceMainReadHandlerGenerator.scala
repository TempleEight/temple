package temple.generate.server.go.service.main

import temple.ast.Metadata.Readable
import temple.generate.CRUD.Read
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.service.main.GoServiceMainGenerator.{generateDAOReadCall, generateDAOReadInput}
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainReadHandlerGenerator {

  private def generateDAOCallBlock(root: ServiceRoot): String =
    mkCode.doubleLines(
      generateDAOReadInput(root),
      generateInvokeBeforeHookBlock(root, ListMap(), Read),
      mkCode.lines(
        generateDAOReadCall(root),
        generateDAOCallErrorBlock(root),
      ),
    )

  /** Generate the read handler function */
  private[main] def generateReadHandler(root: ServiceRoot, responseMap: ListMap[String, String]): String =
    mkCode(
      generateHandlerDecl(root, Read),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          when(root.projectUsesAuth) { generateExtractAuthBlock(root.readable == Readable.This) },
          generateExtractIDBlock(root.decapitalizedName),
          when(root.readable == Readable.This) { generateCheckAuthorizationBlock(root) },
          generateDAOCallBlock(root),
          generateJSONResponse(s"read${root.name}", responseMap),
        ),
      ),
    )
}
