package temple.generate.server.go.service.main

import temple.generate.CRUD.List
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.main.GoServiceMainHandlersGenerator.{generateHandlerDecl, generateExtractAuthBlock, generateHTTPError}
import temple.generate.server.{CreatedByAttribute, ServiceRoot}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainListHandlerGenerator {

  /** Generate the list handler function */
  private[main] def generateListHandler(root: ServiceRoot, responseMap: ListMap[String, String]): String = {
    // Whether enumerating by created_by field or not
    val byCreator = root.createdByAttribute match {
      case CreatedByAttribute.None                  => false
      case _: CreatedByAttribute.EnumerateByCreator => true
      case _: CreatedByAttribute.EnumerateByAll     => false
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

}
