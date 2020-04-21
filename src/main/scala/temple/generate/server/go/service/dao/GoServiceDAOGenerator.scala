package temple.generate.server.go.service.dao

import temple.ast.AttributeType
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common.GoCommonDAOGenerator
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.server.{AttributesRoot, ServiceName}
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

  private[dao] def generateDAOFunctionName(block: ServiceName, operation: CRUD): String =
    s"$operation${block.name}"

  private[service] def generateDatastoreObjectStruct(block: AttributesRoot): String = {
    val idMap = ListMap(block.idAttribute.name.toUpperCase -> generateGoType(AttributeType.UUIDType))
    val createdByMap = block.createdByAttribute.map { createdBy =>
      (createdBy.name.capitalize -> generateGoType(AttributeType.UUIDType))
    }
    val parentMap = block.parentAttribute.map { parent =>
      (parent.name.capitalize -> generateGoType(AttributeType.UUIDType))
    }
    val attributesMap = block.attributes.map {
      case (name, attribute) => name.capitalize -> generateGoType(attribute.attributeType)
    }

    mkCode.lines(
      s"// ${block.name} encapsulates the object stored in the datastore",
      mkCode(
        s"type ${block.name} struct",
        CodeWrap.curly.tabbed(
          // Compose struct fields
          CodeUtils.pad(idMap ++ createdByMap ++ parentMap ++ attributesMap),
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

  private def generateError(block: AttributesRoot): String =
    mkCode.lines(
      s"// Err${block.name}NotFound is returned when a ${block.decapitalizedName} for the provided ID was not found",
      s"type Err${block.name}NotFound string",
      "",
      mkCode(
        s"func (e Err${block.name}NotFound) Error() string",
        CodeWrap.curly.tabbed(s"""return fmt.Sprintf("${block.decapitalizedName} not found with ID %s", string(e))"""),
      ),
    )

  private[service] def generateErrors(root: ServiceRoot): String =
    mkCode.doubleLines(
      "package dao",
      """import "fmt"""",
      root.blockIterator.map(generateError),
    )
}
