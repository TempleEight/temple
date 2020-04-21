package temple.generate.server.go.service.dao

import temple.ast.AttributeType
import temple.ast.Metadata.Readable
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot
import temple.generate.server.go.common.GoCommonGenerator.generateGoType
import temple.generate.server.go.service.dao.GoServiceDAOGenerator.generateDAOFunctionName
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils

object GoServiceDAOInputStructsGenerator {

  private def generateStructCommentSubstring(block: AttributesRoot, operation: CRUD): String =
    operation match {
      case List                            => s"read a ${block.decapitalizedName} list"
      case Create | Read | Update | Delete => s"${operation.toString.toLowerCase} a single ${block.decapitalizedName}"
      case Identify                        => s"identify the current ${block.decapitalizedName}"
    }

  private def generateStruct(block: AttributesRoot, operation: CRUD): String = {
    val structName       = s"${generateDAOFunctionName(block, operation)}Input"
    val commentSubstring = generateStructCommentSubstring(block, operation)

    // We assume identifier is an acronym, so we upper case it
    lazy val id = Iterable(block.idAttribute.name.toUpperCase -> generateGoType(AttributeType.UUIDType))

    // Note we use the createdBy input name, rather than name
    lazy val createdBy = block.createdByAttribute.map { createdBy =>
      createdBy.inputName.capitalize -> generateGoType(AttributeType.UUIDType)
    }

    lazy val parent = block.parentAttribute.map { parentAttribute =>
      parentAttribute.name.capitalize -> generateGoType(AttributeType.UUIDType)
    }

    lazy val attributesMap = block.storedAttributes.map {
      case (name, attribute) => (name.capitalize, generateGoType(attribute.attributeType))
    }

    mkCode.lines(
      s"// $structName encapsulates the information required to $commentSubstring in the datastore",
      mkCode(
        s"type $structName struct",
        CodeWrap.curly.tabbed(
          CodeUtils.pad(
            operation match {
              // Compose struct fields for each operation
              case List                     => createdBy ++ parent
              case Create                   => id ++ createdBy ++ parent ++ attributesMap
              case Read | Delete | Identify => id
              case Update                   => id ++ attributesMap
            },
          ),
        ),
      ),
    )
  }

  private[service] def generateStructs(
    block: AttributesRoot,
  ): String =
    mkCode.doubleLines(
      // Generate input struct for each operation, except for List when not enumerating by creator
      for (operation <- block.operations.toSeq
           if operation != List || block.readable == Readable.This || block.isStruct)
        yield generateStruct(block, operation),
    )
}
