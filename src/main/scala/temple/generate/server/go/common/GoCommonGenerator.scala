package temple.generate.server.go.common

import temple.ast.AttributeType
import temple.ast.AttributeType._
import temple.generate.server.ServiceRoot
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils

import scala.collection.immutable.ListMap

object GoCommonGenerator {

  private[go] def generateMod(module: String): String = mkCode.doubleLines(s"module $module", "go 1.13")

  private[go] def generatePackage(packageName: String): String = s"package $packageName"

  private[go] def generateGoType(attributeType: AttributeType): String =
    attributeType match {
      case UUIDType                                       => "uuid.UUID"
      case IntType(_, Some(min), p) if p <= 1 && min >= 0 => "uint8"
      case IntType(_, Some(min), p) if p <= 2 && min >= 0 => "uint16"
      case IntType(_, Some(min), p) if p <= 4 && min >= 0 => "uint32"
      case IntType(_, Some(min), _) if min >= 0           => "uint64"
      case IntType(_, _, p) if p <= 1                     => "int8"
      case IntType(_, _, p) if p <= 2                     => "int16"
      case IntType(_, _, p) if p <= 4                     => "int32"
      case IntType(_, _, _)                               => "int64"
      case FloatType(_, _, p) if p <= 4                   => "float32"
      case FloatType(_, _, _)                             => "float64"
      case StringType(_, _)                               => "string"
      case BoolType                                       => "bool"
      case DateType                                       => "time.Time"
      case TimeType                                       => "time.Time"
      case DateTimeType                                   => "time.Time"
      case BlobType(Some(size))                           => s"[$size]byte"
      case BlobType(_)                                    => "[]byte"
    }

  private[go] def generateCheckAndReturnError(returnValues: String*): String =
    mkCode(
      "if err != nil",
      CodeWrap.curly.tabbed(
        mkCode(
          "return",
          mkCode.list(returnValues, "err"),
        ),
      ),
    )

  /** Generate constant */
  private[go] def genConst(identifier: String, expr: String): String =
    mkCode(
      "const",
      identifier,
      "=",
      expr,
    )

  /** Generate struct */
  private[go] def genStruct(identifier: String, fields: ListMap[String, String]): String =
    mkCode(
      "type",
      identifier,
      "struct",
      CodeWrap.curly.tabbed(
        CodeUtils.pad(fields),
      ),
    )

  /** Generate declaration and assignment statement */
  private[go] def genDeclareAndAssign(expr: String, identifiers: String*): String =
    mkCode.list(identifiers) + " := " + expr // mkCode doesn't put a space before the colon

  /** Generate assignment */
  private[go] def genAssign(expr: String, identifiers: String*): String =
    mkCode(
      mkCode.list(identifiers),
      "=",
      expr,
    )

  /** Generate variable declaration */
  private[go] def genVar(identifier: String, typ: String): String =
    mkCode(
      "var",
      identifier,
      typ,
    )

  /** Generate function call */
  private[go] def genFunctionCall(name: String, args: String*): String =
    CodeWrap.parens.prefix(name).list(args)

  /** Generate for loop with expression */
  private[go] def genForLoop(expr: String, body: String): String =
    mkCode(
      "for",
      expr,
      CodeWrap.curly.tabbed(body),
    )

  /** Generate return statement */
  private[go] def genReturn(exprs: String*): String =
    mkCode(
      "return",
      mkCode.list(exprs),
    )
}
