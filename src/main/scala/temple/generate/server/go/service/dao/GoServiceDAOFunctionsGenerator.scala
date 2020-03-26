package temple.generate.server.go.service.dao

import temple.ast.{Annotation, Attribute}
import temple.generate.CRUD.{CRUD, Create, Delete, List, Read, Update}
import temple.generate.server.CreatedByAttribute.EnumerateByCreator
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.server.go.service.dao.GoServiceDAOInterfaceGenerator.generateInterfaceFunction
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.{doubleQuote, tabIndent}

import scala.collection.immutable.ListMap

object GoServiceDAOFunctionsGenerator {

  private def generateDAOFunctionComment(
    serviceName: String,
    operation: CRUD,
    createdByAttribute: CreatedByAttribute,
  ): String =
    mkCode(
      "//",
      generateDAOFunctionName(operation, serviceName),
      operation match {
        case List   => s"returns a list containing every $serviceName"
        case Create => s"creates a new $serviceName"
        case Read   => s"returns the $serviceName"
        case Update => s"updates the $serviceName"
        case Delete => s"deletes the $serviceName"
      },
      "in the datastore",
      operation match {
        case List =>
          createdByAttribute match {
            case _: EnumerateByCreator => "for a given ID"
            case _                     => ""
          }
        case Create => s", returning the newly created $serviceName"
        case Read   => "for a given ID"
        case Update => s"for a given ID, returning the newly updated $serviceName"
        case Delete => "for a given ID"
      },
    )

  private def generateQueryArgs(
    operation: CRUD,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): Seq[String] = {
    val prefix  = "input"
    lazy val id = Seq(s"$prefix.${idAttribute.name.toUpperCase()}")
    lazy val createdBy = createdByAttribute match {
      case EnumerateByCreator(inputName, _, _) => Seq(s"$prefix.${inputName.capitalize}")
      case _                                   => Seq.empty
    }
    lazy val filteredAttributes = (attributes.collect {
      case (name, attribute) if !attribute.accessAnnotation.contains(Annotation.ServerSet) =>
        s"$prefix.${name.capitalize}"
    }).toSeq

    operation match {
      case List          => createdBy
      case Create        => id ++ createdBy ++ filteredAttributes
      case Read | Delete => id
      case Update        => filteredAttributes ++ id
    }
  }

  private def generateQueryBlockErrorHandling(
    serviceName: String,
    operation: CRUD,
    idAttribute: IDAttribute,
  ): Option[String] =
    operation match {
      case List => Some(generateCheckAndReturnError("nil"))
      case Delete =>
        Some(
          mkCode(
            generateCheckAndReturnError(),
            mkCode(
              "else if rowsAffected == 0",
              CodeWrap.curly.tabbed(
                s"return Err${serviceName.capitalize}NotFound(input.${idAttribute.name.toUpperCase()}.String())",
              ),
            ),
          ),
        )
      case _ => None
    }

  private def generateQueryBlock(
    serviceName: String,
    operation: CRUD,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
    query: String,
  ): String = {
    val identifiers = operation match {
      case List                   => Seq("rows", "err")
      case Create | Read | Update => Seq("row")
      case Delete                 => Seq("rowsAffected", "err")
    }

    val value = genFunctionCall(
      operation match {
        case List                   => "executeQueryWithRowResponses"
        case Create | Read | Update => "executeQueryWithRowResponse"
        case Delete                 => "executeQuery"
      },
      Seq("dao.DB", doubleQuote(query)) ++
      generateQueryArgs(operation, idAttribute, createdByAttribute, attributes): _*,
    )

    mkCode.lines(
      genDeclareAndAssign(value, identifiers: _*),
      generateQueryBlockErrorHandling(serviceName, operation, idAttribute),
    )
  }

  private def generateScan(
    serviceName: String,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): String =
    CodeWrap.parens
      .prefix("Scan")
      .list(
        s"&$serviceName.${idAttribute.name.toUpperCase()}",
        createdByAttribute match {
          case CreatedByAttribute.None => None
          case enumerating: CreatedByAttribute.Enumerating =>
            Some(s"&${serviceName}.${enumerating.name.capitalize}")
        },
        attributes.map { case (name, _) => s"&$serviceName.${name.capitalize}" },
      )

  private def generateListScanBlock(
    serviceName: String,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
    scanFunctionCall: String,
  ): String =
    mkCode.lines(
      genDeclareAndAssign(s"make([]${serviceName.capitalize}, 0)", s"${serviceName}List"),
      genForLoop(
        "rows.Next()",
        mkCode.lines(
          genVar(serviceName, serviceName.capitalize),
          genAssign(s"rows.$scanFunctionCall", "err"),
          generateCheckAndReturnError("nil"),
          genAssign(genFunctionCall("append", s"${serviceName}List", serviceName), s"${serviceName}List"),
        ),
      ),
      genAssign("rows.Err()", "err"),
      generateCheckAndReturnError("nil"),
    )

  private def generateCreateScanBlock(serviceName: String, scanFunctionCall: String): String =
    mkCode.lines(
      genVar(serviceName, serviceName.capitalize),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      generateCheckAndReturnError("nil"),
    )

  private def generateReadUpdateScanBlock(
    serviceName: String,
    idAttribute: IDAttribute,
    scanFunctionCall: String,
  ): String =
    mkCode.lines(
      genVar(serviceName, serviceName.capitalize),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      mkCode(
        "if err != nil",
        CodeWrap.curly.tabbed(
          mkCode(
            "switch err",
            CodeWrap.curly.noIndent(
              "case sql.ErrNoRows:",
              tabIndent(
                genReturn(
                  "nil",
                  s"Err${serviceName.capitalize}NotFound(input.${idAttribute.name.toUpperCase()}.String())",
                ),
              ),
              "default:",
              tabIndent(
                genReturn("nil", "err"),
              ),
            ),
          ),
        ),
      ),
    )

  private def generateScanBlock(
    serviceName: String,
    operation: CRUD,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): Option[String] = {
    val scanFunctionCall = generateScan(serviceName, idAttribute, createdByAttribute, attributes)
    operation match {
      case List =>
        Some(generateListScanBlock(serviceName, idAttribute, createdByAttribute, attributes, scanFunctionCall))
      case Create        => Some(generateCreateScanBlock(serviceName, scanFunctionCall))
      case Read | Update => Some(generateReadUpdateScanBlock(serviceName, idAttribute, scanFunctionCall))
      case Delete        => None
    }
  }

  private def generateReturnExprs(serviceName: String, operation: CRUD): Seq[String] =
    operation match {
      case List                   => Seq(s"&${serviceName}List", "nil")
      case Create | Read | Update => Seq(s"&$serviceName", "nil")
      case Delete                 => Seq("nil")
    }

  private def generateDAOFunction(
    serviceName: String,
    operation: CRUD,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
    query: String,
  ): String =
    mkCode.lines(
      generateDAOFunctionComment(serviceName, operation, createdByAttribute),
      mkCode(
        "func (dao *DAO)",
        generateInterfaceFunction(serviceName, operation, createdByAttribute),
        CodeWrap.curly.tabbed(
          mkCode.doubleLines(
            generateQueryBlock(serviceName, operation, idAttribute, createdByAttribute, attributes, query),
            generateScanBlock(serviceName, operation, idAttribute, createdByAttribute, attributes),
            genReturn(generateReturnExprs(serviceName, operation): _*),
          ),
        ),
      ),
    )

  private[service] def generateDAOFunctions(
    serviceName: String,
    opQueries: ListMap[CRUD, String],
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): String =
    mkCode.doubleLines(
      for ((operation, query) <- opQueries)
        yield generateDAOFunction(serviceName, operation, idAttribute, createdByAttribute, attributes, query),
    )
}
