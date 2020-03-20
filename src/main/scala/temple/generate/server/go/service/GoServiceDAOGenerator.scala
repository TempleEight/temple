package temple.generate.server.go.service

import temple.ast.{Annotation, Attribute, AttributeType}
import temple.generate.CRUD
import temple.generate.CRUD._
import temple.generate.server.ServiceGenerator.verb
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils
import temple.utils.FileUtils
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
        //doubleQuote("fmt"),
        when(containsTime) { doubleQuote("time") },
        "",
        //doubleQuote(s"$module/util"),
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

  private def generateDAOFunctionName(operation: CRUD, serviceName: String): String =
    s"${verb(operation)}${serviceName.capitalize}"

  private def generateDatastoreInterfaceFunction(
    serviceName: String,
    operation: CRUD,
    enumByCreatedBy: Boolean,
  ): String = {
    val functionName = generateDAOFunctionName(operation, serviceName)
    val functionArgs = if (enumByCreatedBy || operation != CRUD.ReadAll) s"input ${functionName}Input" else ""
    mkCode(
      s"$functionName($functionArgs)",
      generateDatastoreInterfaceFunctionReturnType(serviceName, operation),
    )
  }

  private[service] def generateDatastoreInterface(
    serviceName: String,
    operations: Set[CRUD],
    enumByCreatedBy: Boolean,
  ): String =
    mkCode.lines(
      "// Datastore provides the interface adopted by the DAO, allowing for mocking",
      mkCode(
        "type Datastore interface",
        CodeWrap.curly.tabbed(
          for (operation <- CRUD.values if operations.contains(operation))
            yield generateDatastoreInterfaceFunction(serviceName, operation, enumByCreatedBy),
        ),
      ),
    )

  private[service] def generateDatastoreObjectStruct(
    serviceName: String,
    idAttribute: IDAttribute,
    createdByAttribute: Option[CreatedByAttribute],
    attributes: ListMap[String, Attribute],
  ): String = {

    val idListMap = ListMap(idAttribute.name.toUpperCase -> generateGoType(idAttribute.attributeType))
    val createdByListMap = createdByAttribute.map(createdByAttribute =>
      createdByAttribute.name.capitalize -> generateGoType(createdByAttribute.attributeType),
    )
    val attributesListMap = attributes.map {
      case (name, attribute) => (name.capitalize -> generateGoType(attribute.attributeType))
    }

    val structFields = idListMap ++
      createdByListMap ++
      attributesListMap

    mkCode.lines(
      s"// ${serviceName.capitalize} encapsulates the object stored in the datastore",
      mkCode(
        s"type ${serviceName.capitalize} struct",
        CodeWrap.curly.tabbed(
          CodeUtils.pad(structFields),
        ),
      ),
    )
  }

  private def generateInputStructCommentSubstring(operation: CRUD, serviceName: String): String =
    operation match {
      case ReadAll                         => s"read a $serviceName list"
      case Create | Read | Update | Delete => s"${verb(operation).toLowerCase()} a single $serviceName"
    }

  private def generateInputStruct(
    serviceName: String,
    operation: CRUD,
    idAttribute: IDAttribute,
    createdByAttribute: Option[CreatedByAttribute],
    attributes: ListMap[String, Attribute],
  ): String = {
    val structName = s"${generateDAOFunctionName(operation, serviceName)}Input"
    val idListMap  = ListMap(idAttribute.name.toUpperCase -> generateGoType(idAttribute.attributeType))

    val createdByVal = createdByAttribute.getOrElse {
      throw new Exception(
        "Programmer error: createdByAttribute should be present because input struct for readAll " +
        "operation should only be generated if enumerating by creator",
      )
    }
    val createdByListMap = ListMap(
      createdByVal.inputName.capitalize -> generateGoType(createdByVal.attributeType),
    )

    // Omit attribute from input struct fields if server set
    val attributesListMap = attributes.collect {
      case (name, attribute) if attribute.accessAnnotation != Some(Annotation.ServerSet) =>
        (name.capitalize, generateGoType(attribute.attributeType))
    }

    mkCode.lines(
      s"// $structName encapsulates the information required to ${generateInputStructCommentSubstring(operation, serviceName)} in the datastore",
      mkCode(
        s"type $structName struct",
        CodeWrap.curly.tabbed(
          CodeUtils.pad(
            operation match {
              // Compose struct fields for each operation
              case ReadAll =>
                createdByListMap
              case Create =>
                idListMap ++
                createdByListMap ++
                attributesListMap
              case Read =>
                idListMap
              case Update =>
                idListMap ++
                attributesListMap
              case Delete =>
                idListMap
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
    createdByAttribute: Option[CreatedByAttribute],
    attributes: ListMap[String, Attribute],
    enumByCreatedBy: Boolean,
  ): String =
    mkCode.doubleLines(
      // Generate input struct for each operation, except for List operation when not enumerating by createdBy
      for (operation <- CRUD.values if operations.contains(operation) && (enumByCreatedBy || operation != CRUD.ReadAll))
        yield generateInputStruct(serviceName, operation, idAttribute, createdByAttribute, attributes),
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
