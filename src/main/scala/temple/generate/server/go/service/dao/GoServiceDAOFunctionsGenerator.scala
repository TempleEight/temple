package temple.generate.server.go.service.dao

import temple.ast.{Annotation, Attribute}
import temple.generate.CRUD
import temple.generate.CRUD.{Create, Delete, List, Read, Update}
import temple.generate.server.CreatedByAttribute.EnumerateByCreator
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.server.go.service.dao.GoServiceDAOInterfaceGenerator.generateInterfaceFunction
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote

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
      case CRUD.List               => createdBy
      case CRUD.Create             => id ++ createdBy ++ filteredAttributes
      case CRUD.Read | CRUD.Delete => id
      case CRUD.Update             => filteredAttributes ++ id
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

    val value = genCallFunction(
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

  private def generateQueryBlockReturn(serviceName: String, operation: CRUD): Seq[String] =
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
            genReturn(generateQueryBlockReturn(serviceName, operation): _*),
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
