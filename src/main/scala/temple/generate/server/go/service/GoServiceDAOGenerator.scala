package temple.generate.server.go.service

import temple.ast.{Attribute, AttributeType}
import temple.generate.CRUD
import temple.generate.server.ServiceGenerator.verb
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import temple.generate.CRUD.ReadAll
import temple.generate.CRUD.Create
import temple.generate.CRUD.Read
import temple.generate.CRUD.Update
import temple.generate.CRUD.Delete

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

  private def generateDatastoreInterfaceFunctionReturnType(serviceName: String, operation: CRUD): String =
    operation match {
      case ReadAll                => s"(*[]${serviceName.capitalize}, error)"
      case Create | Read | Update => s"(*${serviceName.capitalize}, error)"
      case Delete                 => "error"
    }

  private def generateDatastoreInterfaceFunction(serviceName: String, operation: CRUD): String = {
    val functionName = s"${verb(operation)}${serviceName.capitalize}"
    mkCode(
      s"$functionName(input ${functionName}Input)",
      generateDatastoreInterfaceFunctionReturnType(serviceName, operation),
    )
  }

  private[service] def generateDatastoreInterface(serviceName: String, operations: Set[CRUD]): String =
    mkCode.lines(
      "// Datastore provides the interface adopted by the DAO, allowing for mocking",
      mkCode(
        "type Datastore interface",
        CodeWrap.curly.tabbed(
          for (operation <- CRUD.values if operations.contains(operation))
            yield generateDatastoreInterfaceFunction(serviceName, operation),
        ),
      ),
    )

  private[service] def generateStructs(): String =
    mkCode.lines(
      "// DAO encapsulates access to the database",
      s"type DAO struct ${CodeWrap.curly.tabbed("DB *sql.DB")}",
    )

  private[service] def generateInit(): String =
    FileUtils.readResources("go/genFiles/common/dao/init.go.snippet").stripLineEnd

  private[service] def generateErrors(serviceName: String): String =
    mkCode.lines(
      "package dao",
      "",
      """import "fmt"""",
      "",
      s"// Err${serviceName.capitalize}NotFound is returned when a $serviceName for the provided ID was not found",
      s"type Err${serviceName.capitalize}NotFound int64",
      "",
      s"func (e Err${serviceName.capitalize}NotFound) Error() string ${CodeWrap.curly
        .tabbed(s"""return fmt.Sprintf("$serviceName not found with ID %d", e)""")}",
    )
}
