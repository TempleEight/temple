package temple.generate.server.go.service.dao

import temple.ast.Attribute
import temple.generate.server.CreatedByAttribute.EnumerateByCreator
import temple.generate.server.go.common.GoCommonGenerator.generateCheckAndReturnError
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.collection.immutable.ListMap

object GoServiceDAOListFunctionGenerator {

  private def generateQueryBlock(
    createdByAttribute: CreatedByAttribute,
    query: String,
  ): String =
    mkCode.lines(
      CodeWrap.parens
        .prefix("rows, err := executeQueryWithRowResponses")
        .list(
          "dao.DB",
          doubleQuote(query),
          createdByAttribute match {
            case EnumerateByCreator(inputName, _, _) => s"input.${inputName.capitalize}"
            case _                                   => ""
          },
        ),
      generateCheckAndReturnError("nil"),
    )

  private def generateScanBlock(
    serviceName: String,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): String = {
    val scanStatement = CodeWrap.parens
      .prefix("err = rows.Scan")
      .list(
        s"&$serviceName.${idAttribute.name.toUpperCase()}",
        createdByAttribute match {
          case CreatedByAttribute.None => None
          case enumerating: CreatedByAttribute.Enumerating =>
            Some(s"&${serviceName}.${enumerating.name.capitalize}")
        },
        attributes.map { case (name, _) => s"&$serviceName.${name.capitalize}" },
      )

    mkCode.lines(
      s"${serviceName}List := make([]${serviceName.capitalize}, 0)",
      mkCode(
        "for rows.Next()",
        CodeWrap.curly.tabbed(
          s"var $serviceName ${serviceName.capitalize}",
          scanStatement,
          generateCheckAndReturnError("nil"),
          s"${serviceName}List = append(${serviceName}List, $serviceName)",
        ),
      ),
      "err = rows.Err()",
      generateCheckAndReturnError("nil"),
    )
  }

  private[dao] def generateDAOListFunctionBody(
    serviceName: String,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
    query: String,
  ): String =
    mkCode.doubleLines(
      generateQueryBlock(createdByAttribute, query),
      generateScanBlock(serviceName, idAttribute, createdByAttribute, attributes),
      s"return &${serviceName}List, nil",
    )
}
