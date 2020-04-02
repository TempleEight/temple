package temple.generate.server.go.service.dao

import temple.ast.Annotation
import temple.generate.CRUD.{CRUD, Create, Delete, List, Read, Update}
import temple.generate.server.CreatedByAttribute.EnumerateByCreator
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.server.go.service.dao.GoServiceDAOInterfaceGenerator.generateInterfaceFunction
import temple.generate.server.{CreatedByAttribute, ServiceRoot}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.collection.immutable.ListMap

object GoServiceDAOFunctionsGenerator {

  private def generateDAOFunctionComment(root: ServiceRoot, operation: CRUD): String =
    mkCode(
      "//",
      generateDAOFunctionName(root, operation),
      operation match {
        case List   => s"returns a list containing every ${root.decapitalizedName}"
        case Create => s"creates a new ${root.decapitalizedName}"
        case Read   => s"returns the ${root.decapitalizedName}"
        case Update => s"updates the ${root.decapitalizedName}"
        case Delete => s"deletes the ${root.decapitalizedName}"
      },
      "in the datastore",
      operation match {
        case List =>
          root.createdByAttribute match {
            case _: EnumerateByCreator => "for a given ID"
            case _                     => ""
          }
        case Create => s", returning the newly created ${root.decapitalizedName}"
        case Read   => "for a given ID"
        case Update => s"for a given ID, returning the newly updated ${root.decapitalizedName}"
        case Delete => "for a given ID"
      },
    )

  private def generateQueryArgs(root: ServiceRoot, operation: CRUD): Seq[String] = {
    val prefix  = "input"
    lazy val id = Seq(s"$prefix.${root.idAttribute.name.toUpperCase}")
    lazy val createdBy = root.createdByAttribute match {
      case EnumerateByCreator(inputName, _) => Seq(s"$prefix.${inputName.capitalize}")
      case _                                => Seq.empty
    }
    lazy val filteredAttributes = root.attributes.collect {
      case (name, attribute) if !attribute.accessAnnotation.contains(Annotation.ServerSet) =>
        s"$prefix.${name.capitalize}"
    }.toSeq

    operation match {
      case List          => createdBy
      case Create        => id ++ createdBy ++ filteredAttributes
      case Read | Delete => id
      case Update        => filteredAttributes ++ id
    }
  }

  private def generateQueryBlockErrorHandling(root: ServiceRoot, operation: CRUD): Option[String] =
    operation match {
      case List =>
        Some(generateCheckAndReturnError("nil"))
      case Delete =>
        Some(
          mkCode(
            generateCheckAndReturnError(),
            "else if rowsAffected == 0",
            CodeWrap.curly.tabbed(
              s"return Err${root.name}NotFound(input.${root.idAttribute.name.toUpperCase}.String())",
            ),
          ),
        )
      case _ =>
        None
    }

  private def generateQueryBlock(root: ServiceRoot, operation: CRUD, query: String): String = {
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
      generateQueryArgs(root, operation): _*,
    )

    mkCode.lines(
      genDeclareAndAssign(value, identifiers: _*),
      generateQueryBlockErrorHandling(root, operation),
    )
  }

  private def generateScan(root: ServiceRoot): String =
    CodeWrap.parens
      .prefix("Scan")
      .list(
        s"&${root.decapitalizedName}.${root.idAttribute.name.toUpperCase}",
        root.createdByAttribute match {
          case CreatedByAttribute.None => None
          case enumerating: CreatedByAttribute.Enumerating =>
            Some(s"&${root.decapitalizedName}.${enumerating.name.capitalize}")
        },
        root.attributes.map { case (name, _) => s"&${root.decapitalizedName}.${name.capitalize}" },
      )

  private def generateListScanBlock(root: ServiceRoot, scanFunctionCall: String): String =
    mkCode.lines(
      genDeclareAndAssign(s"make([]${root.name}, 0)", s"${root.decapitalizedName}List"),
      genForLoop(
        "rows.Next()",
        mkCode.lines(
          genVar(root.decapitalizedName, root.name),
          genAssign(s"rows.$scanFunctionCall", "err"),
          generateCheckAndReturnError("nil"),
          genAssign(
            genFunctionCall("append", s"${root.decapitalizedName}List", root.decapitalizedName),
            s"${root.decapitalizedName}List",
          ),
        ),
      ),
      genAssign("rows.Err()", "err"),
      generateCheckAndReturnError("nil"),
    )

  private def generateCreateScanBlock(root: ServiceRoot, scanFunctionCall: String): String =
    mkCode.lines(
      genVar(root.decapitalizedName, root.name),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      generateCheckAndReturnError("nil"),
    )

  private def generateReadUpdateScanBlock(root: ServiceRoot, scanFunctionCall: String): String =
    mkCode.lines(
      genVar(root.decapitalizedName, root.name),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      mkCode(
        "if err != nil",
        CodeWrap.curly.tabbed(
          genSwitch(
            "err",
            ListMap(
              "sql.ErrNoRows" -> genReturn(
                "nil",
                s"Err${root.name}NotFound(input.${root.idAttribute.name.toUpperCase}.String())",
              ),
            ),
            genReturn("nil", "err"),
          ),
        ),
      ),
    )

  private def generateScanBlock(root: ServiceRoot, operation: CRUD): Option[String] = {
    val scanFunctionCall = generateScan(root)
    operation match {
      case List =>
        Some(generateListScanBlock(root, scanFunctionCall))
      case Create        => Some(generateCreateScanBlock(root, scanFunctionCall))
      case Read | Update => Some(generateReadUpdateScanBlock(root, scanFunctionCall))
      case Delete        => None
    }
  }

  private def generateReturnExprs(root: ServiceRoot, operation: CRUD): Seq[String] =
    operation match {
      case List                   => Seq(s"&${root.decapitalizedName}List", "nil")
      case Create | Read | Update => Seq(s"&${root.decapitalizedName}", "nil")
      case Delete                 => Seq("nil")
    }

  private def generateDAOFunction(root: ServiceRoot, operation: CRUD, query: String): String =
    mkCode.lines(
      generateDAOFunctionComment(root, operation),
      mkCode(
        "func (dao *DAO)",
        generateInterfaceFunction(root, operation),
        CodeWrap.curly.tabbed(
          mkCode.doubleLines(
            generateQueryBlock(root, operation, query),
            generateScanBlock(root, operation),
            genReturn(generateReturnExprs(root, operation): _*),
          ),
        ),
      ),
    )

  private[service] def generateDAOFunctions(root: ServiceRoot): String =
    mkCode.doubleLines(
      for ((operation, query) <- root.opQueries)
        yield generateDAOFunction(root, operation, query),
    )
}
