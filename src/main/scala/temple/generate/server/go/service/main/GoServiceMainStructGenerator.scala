package temple.generate.server.go.service.main

import temple.ast.AttributeType.{BlobType, DateTimeType, DateType, TimeType}
import temple.ast.{Annotation, Attribute, AttributeType}
import temple.generate.CRUD
import temple.generate.CRUD.{CRUD, Create, Read, Update}
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator.{genStruct, genStructWithAnnotations, generateGoType}
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.doubleQuote

import scala.Option.when
import scala.collection.immutable.ListMap

object GoServiceMainStructGenerator {

  private[service] def generateEnvStruct(usesComms: Boolean): String =
    mkCode.lines(
      "// env defines the environment that requests should be executed within",
      genStruct("env", ListMap("dao" -> "dao.Datastore") ++ when(usesComms) { "comm" -> "comm.Comm" }),
    )

  private def generateValidatorAnnotation(attrType: AttributeType): String =
    // TODO: Fill in mapping and handle optionals
    s"`valid:${doubleQuote(
      attrType match {
        case AttributeType.ForeignKey(_)                  => "-"
        case AttributeType.UUIDType                       => "-"
        case AttributeType.BoolType                       => "-"
        case AttributeType.DateType                       => "-"
        case AttributeType.DateTimeType                   => "-"
        case AttributeType.TimeType                       => "-"
        case AttributeType.BlobType(size)                 => "-"
        case AttributeType.StringType(max, min)           => "-"
        case AttributeType.IntType(max, min, precision)   => "-"
        case AttributeType.FloatType(max, min, precision) => "-"
      },
    )}`"

  private def generateRequestStruct(
    root: ServiceRoot,
    operation: CRUD,
    fields: Iterable[(String, String, String)],
  ): String =
    mkCode.lines(
      s"// ${operation.toString.toLowerCase}${root.name.capitalize}Request contains the client-provided information required to ${operation.toString.toLowerCase} a single ${root.name}",
      genStructWithAnnotations(s"${operation.toString.toLowerCase}${root.name.capitalize}Request", fields),
    )

  private[service] def generateRequestStructs(
    root: ServiceRoot,
    operations: Set[CRUD],
    clientAttributes: ListMap[String, Attribute],
  ): String = {
    val fields = clientAttributes.map {
      case (name, attr) =>
        (name.capitalize, s"*${generateGoType(attr.attributeType)}", generateValidatorAnnotation(attr.attributeType))
    }
    mkCode.doubleLines(
      when(operations.contains(CRUD.Create)) {
        generateRequestStruct(root, CRUD.Create, fields)
      },
      when(operations.contains(CRUD.Update)) {
        generateRequestStruct(root, CRUD.Update, fields)
      },
    )
  }

  private def generateGoResponseType(attributeType: AttributeType): String =
    attributeType match {
      case DateType | TimeType | DateTimeType | _: BlobType => "string"
      case _                                                => generateGoType(attributeType)
    }

  private def generateListResponseStructs(root: ServiceRoot, fields: ListMap[String, String]): String =
    mkCode.lines(
      s"// list${root.name.capitalize}Element contains a single ${root.name} list element",
      genStruct(s"list${root.name.capitalize}Element", fields),
      "",
      s"// list${root.name.capitalize}Response contains a single ${root.name} list to be returned to the client",
      genStruct(
        s"list${root.name.capitalize}Response",
        ListMap(s"${root.name.capitalize}List" -> s"[]list${root.name.capitalize}Element"),
      ),
    )

  private def generateResponseStruct(root: ServiceRoot, operation: CRUD, fields: ListMap[String, String]): String =
    mkCode.lines(
      mkCode(
        s"// ${operation.toString.toLowerCase}${root.name.capitalize}Response contains a",
        operation match {
          case Create => s"newly created ${root.name}"
          case Read   => s"single ${root.name}"
          case Update => s"newly updated ${root.name}"
        },
        "to be returned to the client",
      ),
      genStruct(s"${operation.toString.toLowerCase}${root.name.capitalize}Response", fields),
    )

  private[service] def generateResponseStructs(root: ServiceRoot, operations: Set[CRUD]): String = {
    // Response struct fields include ID and user-defined attributes without the @server annotation
    val fields = ListMap(root.idAttribute.name.toUpperCase -> generateGoResponseType(AttributeType.UUIDType)) ++
      root.attributes.collect {
        case (name, attribute) if !attribute.accessAnnotation.contains(Annotation.Server) =>
          (name.capitalize, generateGoResponseType(attribute.attributeType))
      }
    mkCode.doubleLines(
      when(operations.contains(CRUD.List)) { generateListResponseStructs(root, fields) },
      for (operation <- Set(CRUD.Create, CRUD.Read, CRUD.Update) if operations.contains(operation))
        yield generateResponseStruct(root, operation, fields),
    )
  }
}
