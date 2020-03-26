package temple.generate.server.go.service.dao

import temple.ast.Attribute
import temple.generate.CRUD
import temple.generate.CRUD.{Create, Delete, List, Read, Update}
import temple.generate.server.CreatedByAttribute.EnumerateByCreator
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.server.go.service.dao.GoServiceDAOInterfaceGenerator.generateInterfaceFunction
import temple.generate.server.go.service.dao.GoServiceDAOListFunctionGenerator.generateDAOListFunctionBody
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

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
          operation match {
            case List =>
              generateDAOListFunctionBody(serviceName, idAttribute, createdByAttribute, attributes, query)
            case Create | Read | Update => "return nil, nil"
            case Delete                 => "return nil"
          },
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
