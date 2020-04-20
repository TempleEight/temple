package temple.generate.server.go.service.dao

import temple.ast.AttributeType
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonDAOGenerator
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceDAOGenerator {

  private[service] def generateImports(root: ServiceRoot, usesTime: Boolean): String =
    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        doubleQuote("database/sql"),
        doubleQuote("fmt"),
        when(usesTime) { doubleQuote("time") },
        "",
        doubleQuote(s"${root.module}/util"),
        doubleQuote("github.com/google/uuid"),
        "",
        "// pq acts as the driver for SQL requests",
        s"_ ${doubleQuote("github.com/lib/pq")}",
      ),
    )

  private[dao] def generateDAOFunctionName(root: ServiceRoot, operation: CRUD): String =
    s"$operation${root.name}"

  private[service] def generateDatastoreObjectStruct(root: ServiceRoot): String = {
    val idMap = ListMap(root.idAttribute.name.toUpperCase -> generateGoType(AttributeType.UUIDType))
    val createdByMap = root.createdByAttribute.fold(ListMap[String, String]()) { enumerating =>
      ListMap(enumerating.name.capitalize -> generateGoType(AttributeType.UUIDType))
    }
    val attributesMap = root.storedAttributes.map {
      case (name, attribute) => name.capitalize -> generateGoType(attribute.attributeType)
    }

    mkCode.lines(
      s"// ${root.name} encapsulates the object stored in the datastore",
      mkCode(
        s"type ${root.name} struct",
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

  private[service] def generateErrors(root: ServiceRoot): String =
    mkCode.lines(
      "package dao",
      "",
      """import "fmt"""",
      "",
      s"// Err${root.name}NotFound is returned when a ${root.decapitalizedName} for the provided ID was not found",
      s"type Err${root.name}NotFound string",
      "",
      mkCode(
        s"func (e Err${root.name}NotFound) Error() string",
        CodeWrap.curly.tabbed(s"""return fmt.Sprintf("${root.decapitalizedName} not found with ID %s", string(e))"""),
      ),
    )
}
