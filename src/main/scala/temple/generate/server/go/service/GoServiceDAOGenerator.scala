package temple.generate.server.go.service

import temple.ast.{Annotation, Attribute, AttributeType}
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap
import temple.generate.server.go.common.GoCommonDAOGenerator

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
      case List                   => s"(*[]${serviceName.capitalize}, error)"
      case Create | Read | Update => s"(*${serviceName.capitalize}, error)"
      case Delete                 => "error"
    }

  private def generateDAOFunctionName(operation: CRUD, serviceName: String): String =
    s"$operation${serviceName.capitalize}"

  private def generateDatastoreInterfaceFunction(
    serviceName: String,
    operation: CRUD,
    createdByAttribute: CreatedByAttribute,
  ): String = {
    val functionName = generateDAOFunctionName(operation, serviceName)
    val enumeratingByCreator = createdByAttribute match {
      case _: CreatedByAttribute.EnumerateByCreator => true
      case _                                        => false
    }
    val functionArgs = if (enumeratingByCreator || operation != CRUD.List) s"input ${functionName}Input" else ""
    mkCode(
      s"$functionName($functionArgs)",
      generateDatastoreInterfaceFunctionReturnType(serviceName, operation),
    )
  }

  private[service] def generateDatastoreInterface(
    serviceName: String,
    operations: Set[CRUD],
    createdByAttribute: CreatedByAttribute,
  ): String =
    mkCode.lines(
      "// Datastore provides the interface adopted by the DAO, allowing for mocking",
      mkCode(
        "type Datastore interface",
        CodeWrap.curly.tabbed(
          for (operation <- CRUD.values if operations.contains(operation))
            yield generateDatastoreInterfaceFunction(serviceName, operation, createdByAttribute),
        ),
      ),
    )

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

  private def generateInputStructCommentSubstring(operation: CRUD, serviceName: String): String =
    operation match {
      case List                            => s"read a $serviceName list"
      case Create | Read | Update | Delete => s"${operation.toString.toLowerCase} a single $serviceName"
    }

  private def generateInputStruct(
    serviceName: String,
    operation: CRUD,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): String = {
    val structName       = s"${generateDAOFunctionName(operation, serviceName)}Input"
    val commentSubstring = generateInputStructCommentSubstring(operation, serviceName)

    // We assume identifier is an acronym, so we upper case it
    lazy val idMap = ListMap(idAttribute.name.toUpperCase -> generateGoType(idAttribute.attributeType))

    // Note we use the createdBy input name, rather than name
    lazy val createdByMap = createdByAttribute match {
      case CreatedByAttribute.None =>
        ListMap.empty
      case enumerating: CreatedByAttribute.Enumerating =>
        ListMap(enumerating.inputName.capitalize -> generateGoType(enumerating.attributeType))
    }

    // Omit attribute from input struct fields if server set
    lazy val attributesMap = attributes.collect {
      case (name, attribute) if !attribute.accessAnnotation.contains(Annotation.ServerSet) =>
        (name.capitalize, generateGoType(attribute.attributeType))
    }

    mkCode.lines(
      s"// $structName encapsulates the information required to $commentSubstring in the datastore",
      mkCode(
        s"type $structName struct",
        CodeWrap.curly.tabbed(
          CodeUtils.pad(
            operation match {
              // Compose struct fields for each operation
              case List   => createdByMap
              case Create => idMap ++ createdByMap ++ attributesMap
              case Read   => idMap
              case Update => idMap ++ attributesMap
              case Delete => idMap
            },
          ),
        ),
      ),
    )
  }

  private[service] def generateInputStructs(
    serviceName: String,
    operations: Set[CRUD],
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): String = {
    val enumeratingByCreator = createdByAttribute match {
      case _: CreatedByAttribute.EnumerateByCreator => true
      case _                                        => false
    }
    mkCode.doubleLines(
      // Generate input struct for each operation, except for List when not enumerating by creator
      for (operation <- CRUD.values if operations.contains(operation) &&
           (operation != CRUD.List || enumeratingByCreator))
        yield generateInputStruct(serviceName, operation, idAttribute, createdByAttribute, attributes),
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
      s"type Err${serviceName.capitalize}NotFound int64",
      "",
      s"func (e Err${serviceName.capitalize}NotFound) Error() string ${CodeWrap.curly
        .tabbed(s"""return fmt.Sprintf("$serviceName not found with ID %d", e)""")}",
    )
}
