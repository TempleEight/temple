package temple.generate.server.go.service.main

import temple.ast.AttributeType
import temple.ast.AttributeType.{BlobType, DateTimeType, DateType, TimeType}
import temple.generate.CRUD._
import temple.generate.server.AttributesRoot
import temple.generate.server.go.common.GoCommonGenerator.{genStruct, genStructWithAnnotations, generateGoType}
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.{backTick, doubleQuote}

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainStructGenerator {

  private[service] def generateEnvStruct(usesComms: Boolean): String =
    mkCode.lines(
      "// env defines the environment that requests should be executed within",
      genStruct(
        "env",
        ListMap("dao" -> "dao.Datastore", "hook" -> "Hook") ++ when(usesComms) { "comm" -> "comm.Comm" },
      ),
    )

  private def generateValidatorAnnotation(attrType: AttributeType): String =
    s"valid:${doubleQuote(
      attrType match {
        // TODO: Work out what type the validator wants for uuid
        case AttributeType.ForeignKey(_) | AttributeType.UUIDType => "required"
        case AttributeType.BoolType                               => "type(*bool),required"
        case AttributeType.DateType                               => "type(*string),required"
        case AttributeType.DateTimeType                           => "type(*string),rfc3339,required"
        case AttributeType.TimeType                               => "type(*string),required"
        case AttributeType.BlobType(_)                            => "type(*string),base64,required"
        case AttributeType.StringType(Some(max), Some(min)) =>
          s"type(*string),required,stringlength($min|$max)"
        case AttributeType.StringType(_, _) =>
          // TODO: Requires a custom validator
          "type(*string),required"
        case AttributeType.IntType(Some(max), Some(min), _) =>
          s"type(*${generateGoType(attrType)}),required,range($min|$max)"
        case AttributeType.IntType(_, _, _) =>
          // TODO: Requires a custom validator
          s"type(*${generateGoType(attrType)}),required"
        case AttributeType.FloatType(Some(max), Some(min), _) =>
          s"type(*${generateGoType(attrType)}),required,range($min|$max)"
        case AttributeType.FloatType(_, _, _) =>
          // TODO: Requires a custom validator
          s"type(*${generateGoType(attrType)}),required"
      },
    )}"

  private def generateJSONAnnotation(name: String): String =
    s"json:${doubleQuote(name)}"

  private def generateRequestAnnotation(name: String, attrType: AttributeType): String =
    backTick(mkCode(generateJSONAnnotation(name), generateValidatorAnnotation(attrType)))

  private def generateRequestStruct(
    block: AttributesRoot,
    operation: CRUD,
    fields: Iterable[(String, String, String)],
  ): String =
    mkCode.lines(
      s"// ${operation.toString.toLowerCase}${block.name}Request contains the client-provided information " +
      s"required to ${operation.toString.toLowerCase} a single ${block.decapitalizedName}",
      genStructWithAnnotations(s"${operation.toString.toLowerCase}${block.name}Request", fields),
    )

  private[service] def generateRequestStructs(block: AttributesRoot): String = {
    val fields = block.requestAttributes.map {
      case (name, attr) =>
        (
          name.capitalize,
          s"*${generateRequestResponseType(attr.attributeType)}",
          generateRequestAnnotation(name, attr.attributeType),
        )
    }
    mkCode.doubleLines(
      when(block.operations contains Create) {
        generateRequestStruct(block, Create, fields)
      },
      when(block.operations contains Update) {
        generateRequestStruct(block, Update, fields)
      },
    )
  }

  private def generateRequestResponseType(attributeType: AttributeType): String =
    attributeType match {
      case DateType | TimeType | DateTimeType | _: BlobType => "string"
      case _                                                => generateGoType(attributeType)
    }

  private def generateListResponseStructs(block: AttributesRoot, fields: Iterable[(String, String, String)]): String =
    mkCode.lines(
      s"// list${block.name}Element contains a single ${block.decapitalizedName} list element",
      genStructWithAnnotations(s"list${block.name}Element", fields),
      "",
      s"// list${block.name}Response contains a single ${block.decapitalizedName} list to be returned to the client",
      genStruct(
        s"list${block.name}Response",
        ListMap(s"${block.name}List" -> s"[]list${block.name}Element"),
      ),
    )

  private def generateResponseStruct(
    block: AttributesRoot,
    operation: CRUD,
    fields: Iterable[(String, String, String)],
  ): String =
    mkCode.lines(
      mkCode(
        s"// ${operation.toString.toLowerCase}${block.name}Response contains a",
        operation match {
          case Create                   => s"newly created ${block.decapitalizedName}"
          case Read                     => s"single ${block.decapitalizedName}"
          case Update                   => s"newly updated ${block.decapitalizedName}"
          case Delete | Identify | List => throw new MatchError(s"$operation should not have a response struct")
        },
        "to be returned to the client",
      ),
      genStructWithAnnotations(s"${operation.toString.toLowerCase}${block.name}Response", fields),
    )

  private[service] def generateResponseStructs(block: AttributesRoot): String = {
    // Response struct fields include ID and user-defined attributes without the @server annotation
    val fields = Iterable(
        (
          block.idAttribute.name.toUpperCase,
          generateRequestResponseType(AttributeType.UUIDType),
          backTick(generateJSONAnnotation(block.idAttribute.name)),
        ),
      ) ++
      block.attributes.collect {
        case (name, attribute) if attribute.inResponse =>
          (
            name.capitalize,
            generateRequestResponseType(attribute.attributeType),
            backTick(generateJSONAnnotation(name)),
          )
      }
    mkCode.doubleLines(
      when(block.operations contains List) { generateListResponseStructs(block, fields) },
      for (operation <- block.operations intersect Set(Create, Read, Update))
        yield generateResponseStruct(block, operation, fields),
    )
  }
}
