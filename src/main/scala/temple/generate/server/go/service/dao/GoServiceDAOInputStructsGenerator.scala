package temple.generate.server.go.service.dao

import temple.ast.{Annotation, Attribute}
import temple.generate.CRUD
import temple.generate.CRUD.{Create, Delete, List, Read, Update}
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.server.{CreatedByAttribute, IDAttribute}
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils

import scala.collection.immutable.ListMap

object GoServiceDAOInputStructsGenerator {

  private def generateStructCommentSubstring(operation: CRUD, serviceName: String): String =
    operation match {
      case List                            => s"read a $serviceName list"
      case Create | Read | Update | Delete => s"${operation.toString.toLowerCase} a single $serviceName"
    }

  private def generateStruct(
    serviceName: String,
    operation: CRUD,
    idAttribute: IDAttribute,
    createdByAttribute: CreatedByAttribute,
    attributes: ListMap[String, Attribute],
  ): String = {
    val structName       = s"${generateDAOFunctionName(operation, serviceName)}Input"
    val commentSubstring = generateStructCommentSubstring(operation, serviceName)

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

  private[service] def generateStructs(
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
        yield generateStruct(serviceName, operation, idAttribute, createdByAttribute, attributes),
    )
  }
}
