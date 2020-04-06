package temple.generate.server.go.service.dao

import temple.ast.{Annotation, AttributeType}
import temple.generate.CRUD
import temple.generate.CRUD.{CRUD, Create, Delete, List, Read, Update}
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils

import scala.collection.immutable.ListMap

object GoServiceDAOInputStructsGenerator {

  private def generateStructCommentSubstring(root: ServiceRoot, operation: CRUD): String =
    operation match {
      case List                            => s"read a ${root.decapitalizedName} list"
      case Create | Read | Update | Delete => s"${operation.toString.toLowerCase} a single ${root.decapitalizedName}"
    }

  private def generateStruct(root: ServiceRoot, operation: CRUD): String = {
    val structName       = s"${generateDAOFunctionName(root, operation)}Input"
    val commentSubstring = generateStructCommentSubstring(root, operation)

    // We assume identifier is an acronym, so we upper case it
    lazy val idMap = ListMap(root.idAttribute.name.toUpperCase -> generateGoType(AttributeType.UUIDType))

    // Note we use the createdBy input name, rather than name
    lazy val createdByMap = root.createdByAttribute.fold(ListMap[String, String]()) { enumerating =>
      ListMap(enumerating.inputName.capitalize -> generateGoType(AttributeType.UUIDType))
    }

    // Omit attribute from input struct fields if server set
    lazy val attributesMap = root.attributes.collect {
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
    root: ServiceRoot,
    operations: Set[CRUD],
    enumeratingByCreator: Boolean,
  ): String =
    mkCode.doubleLines(
      // Generate input struct for each operation, except for List when not enumerating by creator
      for (operation <- operations.toSeq.sorted if operation != CRUD.List || enumeratingByCreator)
        yield generateStruct(root, operation),
    )
}
