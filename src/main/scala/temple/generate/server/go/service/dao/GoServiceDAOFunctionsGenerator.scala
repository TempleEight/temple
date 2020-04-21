package temple.generate.server.go.service.dao

import temple.ast.Metadata.Readable
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.server.go.service.dao.GoServiceDAOInterfaceGenerator.generateInterfaceFunction
import temple.generate.server.{AttributesRoot, CreatedByAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceDAOFunctionsGenerator {

  private def generateDAOFunctionComment(block: AttributesRoot, operation: CRUD): String =
    mkCode(
      "//",
      generateDAOFunctionName(block, operation),
      operation match {
        case List     => s"returns a list containing every ${block.decapitalizedName}"
        case Create   => s"creates a new ${block.decapitalizedName}"
        case Read     => s"returns the ${block.decapitalizedName}"
        case Update   => s"updates the ${block.decapitalizedName}"
        case Delete   => s"deletes the ${block.decapitalizedName}"
        case Identify => s"returns the ${block.decapitalizedName}"
      },
      "in the datastore",
      operation match {
        case List     => when(block.readable == Readable.This) { "for a given ID" }
        case Create   => s", returning the newly created ${block.decapitalizedName}"
        case Read     => "for a given ID"
        case Update   => s"for a given ID, returning the newly updated ${block.decapitalizedName}"
        case Delete   => "for a given ID"
        case Identify => "for a given ID"
      },
    )

  private def generateQueryArgs(block: AttributesRoot, operation: CRUD): Seq[String] = {
    val prefix  = "input"
    lazy val id = Seq(s"$prefix.${block.idAttribute.name.toUpperCase}")
    lazy val createdBy = block.createdByAttribute match {
      // If for the list operation, only include createdBy argument if readable by this
      case Some(CreatedByAttribute(inputName, _)) if operation != List || block.readable == Readable.This =>
        Seq(s"$prefix.${inputName.capitalize}")
      case _ => Seq.empty
    }
    lazy val parentBlock = when(block.parentAttribute.isDefined) { "input.ParentID" }
    lazy val attributes  = block.storedAttributes.map { case name -> _ => s"$prefix.${name.capitalize}" }.toSeq

    operation match {
      case List                     => createdBy ++ parentBlock
      case Create                   => id ++ createdBy ++ attributes
      case Read | Delete | Identify => id
      case Update                   => attributes ++ id
    }
  }

  private def generateQueryBlockErrorHandling(block: AttributesRoot, operation: CRUD): Option[String] =
    operation match {
      case List =>
        Some(genCheckAndReturnError("nil"))
      case Delete =>
        Some(
          mkCode(
            genCheckAndReturnError(),
            "else if rowsAffected == 0",
            CodeWrap.curly.tabbed(
              s"return Err${block.name}NotFound(input.${block.idAttribute.name.toUpperCase}.String())",
            ),
          ),
        )
      case _ =>
        None
    }

  private def generateQueryBlock(block: AttributesRoot, operation: CRUD, query: String): String = {
    val identifiers = operation match {
      case List                              => Seq("rows", "err")
      case Create | Read | Update | Identify => Seq("row")
      case Delete                            => Seq("rowsAffected", "err")
    }

    val value = genFunctionCall(
      operation match {
        case List                              => "executeQueryWithRowResponses"
        case Create | Read | Update | Identify => "executeQueryWithRowResponse"
        case Delete                            => "executeQuery"
      },
      "dao.DB",
      doubleQuote(query),
      generateQueryArgs(block, operation),
    )

    mkCode.lines(
      genDeclareAndAssign(value, identifiers: _*),
      generateQueryBlockErrorHandling(block, operation),
    )
  }

  private def generateScan(block: AttributesRoot): String =
    CodeWrap.parens
      .prefix("Scan")
      .list(
        s"&${block.decapitalizedName}.${block.idAttribute.name.toUpperCase}",
        block.createdByAttribute.map { enumerating =>
          Some(s"&${block.decapitalizedName}.${enumerating.name.capitalize}")
        },
        block.storedAttributes.map { case (name, _) => s"&${block.decapitalizedName}.${name.capitalize}" },
      )

  private def generateListScanBlock(block: AttributesRoot, scanFunctionCall: String): String =
    mkCode.lines(
      genDeclareAndAssign(s"make([]${block.name}, 0)", s"${block.decapitalizedName}List"),
      genForLoop(
        "rows.Next()",
        mkCode.lines(
          genVar(block.decapitalizedName, block.name),
          genAssign(s"rows.$scanFunctionCall", "err"),
          genCheckAndReturnError("nil"),
          genAssign(
            genFunctionCall("append", s"${block.decapitalizedName}List", block.decapitalizedName),
            s"${block.decapitalizedName}List",
          ),
        ),
      ),
      genAssign("rows.Err()", "err"),
      genCheckAndReturnError("nil"),
    )

  private def generateCreateScanBlock(block: AttributesRoot, scanFunctionCall: String): String =
    mkCode.lines(
      genVar(block.decapitalizedName, block.name),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      genCheckAndReturnError("nil"),
    )

  private def generateReadUpdateScanBlock(block: AttributesRoot, scanFunctionCall: String): String =
    mkCode.lines(
      genVar(block.decapitalizedName, block.name),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      mkCode(
        "if err != nil",
        CodeWrap.curly.tabbed(
          genSwitch(
            "err",
            ListMap(
              "sql.ErrNoRows" -> genReturn(
                "nil",
                s"Err${block.name}NotFound(input.${block.idAttribute.name.toUpperCase}.String())",
              ),
            ),
            genReturn("nil", "err"),
          ),
        ),
      ),
    )

  private def generateScanBlock(block: AttributesRoot, operation: CRUD): Option[String] = {
    val scanFunctionCall = generateScan(block)
    operation match {
      case List                     => Some(generateListScanBlock(block, scanFunctionCall))
      case Create                   => Some(generateCreateScanBlock(block, scanFunctionCall))
      case Read | Update | Identify => Some(generateReadUpdateScanBlock(block, scanFunctionCall))
      case Delete                   => None
    }
  }

  private def generateReturnExprs(block: AttributesRoot, operation: CRUD): Seq[String] =
    operation match {
      case List                              => Seq(s"&${block.decapitalizedName}List", "nil")
      case Create | Read | Update | Identify => Seq(s"&${block.decapitalizedName}", "nil")
      case Delete                            => Seq("nil")
    }

  private def generateDAOFunction(block: AttributesRoot, operation: CRUD, query: String): String = mkCode.lines(
    generateDAOFunctionComment(block, operation),
    mkCode(
      "func (dao *DAO)",
      generateInterfaceFunction(block, operation),
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(
          generateQueryBlock(block, operation, query),
          generateScanBlock(block, operation),
          genReturn(generateReturnExprs(block, operation): _*),
        ),
      ),
    ),
  )

  private[service] def generateDAOFunctions(block: AttributesRoot): String =
    mkCode.doubleLines(
      for ((operation, query) <- block.opQueries)
        yield generateDAOFunction(block, operation, query),
    )
}
