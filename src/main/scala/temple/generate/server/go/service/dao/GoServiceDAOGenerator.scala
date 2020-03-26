package temple.generate.server.go.service.dao

import temple.ast.{Attribute, AttributeType}
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.server.go.common.GoCommonDAOGenerator
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceDAOGenerator {

  private[service] def generateImports(attributes: Map[String, Attribute], module: String): String = {
    // Check if attributes contains an attribute of type date, time or datetime
    val containsTime =
      Set[AttributeType](AttributeType.DateType, AttributeType.TimeType, AttributeType.DateTimeType)
        .intersect(attributes.values.map(_.attributeType).toSet)
        .nonEmpty

    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        doubleQuote("database/sql"),
        doubleQuote("fmt"),
        when(containsTime) { doubleQuote("time") },
        "",
        doubleQuote(s"$module/util"),
        doubleQuote("github.com/google/uuid"),
        "",
        "// pq acts as the driver for SQL requests",
        s"_ ${doubleQuote("github.com/lib/pq")}",
      ),
    )
  }

  private[dao] def generateDAOFunctionName(operation: CRUD, serviceName: String): String =
    s"$operation${serviceName.capitalize}"

  private[service] def generateDatastoreObjectStruct(
    serviceName: String,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): String = {
    val idMap = ListMap(idAttribute.name.toUpperCase -> generateGoType(idAttribute.attributeType))
    val createdByMap = createdByAttribute match {
      case CreatedByAttribute.None =>
        ListMap.empty
      case enumerating: CreatedByAttribute.Enumerating =>
        ListMap(enumerating.name.capitalize -> generateGoType(enumerating.attributeType))
    }
    val attributesMap = attributes.map {
      case (name, attribute) => (name.capitalize -> generateGoType(attribute.attributeType))
    }

    mkCode.lines(
      s"// ${serviceName.capitalize} encapsulates the object stored in the datastore",
      mkCode(
        s"type ${serviceName.capitalize} struct",
        CodeWrap.curly.tabbed(
          // Compose struct fields
          CodeUtils.pad(idMap ++ createdByMap ++ attributesMap),
        ),
      ),
    )
  }

  private[service] def generateQueryFunctions(operations: Set[CRUD]): String =
    mkCode.doubleLines(
      when(operations.contains(List)) { GoCommonDAOGenerator.generateExecuteQueryWithRowResponses() },
      when(operations.intersect(Set(Create, Read, Update)).nonEmpty) {
        GoCommonDAOGenerator.generateExecuteQueryWithRowResponse()
      },
      when(operations.contains(Delete)) { GoCommonDAOGenerator.generateExecuteQuery() },
    )

  private[service] def generateErrors(serviceName: String): String =
    mkCode.lines(
      "package dao",
      "",
      """import "fmt"""",
      "",
      s"// Err${serviceName.capitalize}NotFound is returned when a $serviceName for the provided ID was not found",
      s"type Err${serviceName.capitalize}NotFound string",
      "",
      s"func (e Err${serviceName.capitalize}NotFound) Error() string ${CodeWrap.curly
        .tabbed(s"""return fmt.Sprintf("$serviceName not found with ID %d", string(e))""")}",
    )
}
