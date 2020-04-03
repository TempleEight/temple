package temple.generate.server.go.service.main

import temple.ast.Attribute
import temple.generate.CRUD.Create
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.generateHandlerDecl
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator._

import scala.collection.immutable.ListMap
import scala.Option.when

object GoServiceMainCreateHandlerGenerator {

  /** Generate the checking that incoming request parameters are not nil */
  private def generateRequestNilCheck(root: ServiceRoot, clientAttributes: ListMap[String, Attribute]): String =
    genIf(
      clientAttributes.map { case name -> _ => s"req.${name.capitalize} == nil" }.mkString(" || "),
      mkCode.lines(generateHTTPError("StatusBadRequest", "Missing request parameter(s)"), genReturn()),
    )

  /** Generate the create handler function */
  private[main] def generateCreateHandler(
    root: ServiceRoot,
    clientAttributes: ListMap[String, Attribute],
    usesComms: Boolean,
  ): String =
    mkCode(
      generateHandlerDecl(root, Create),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          generateExtractAuthBlock(),
          generateDecodeRequestBlock(s"create${root.name.capitalize}"),
          generateRequestNilCheck(root, clientAttributes),
          generateValidateStructBlock(),
          when(usesComms) { generateForeignKeyCheckBlocks(root, clientAttributes) },
        ),
      ),
    )
}
