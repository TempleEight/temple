package temple.generate.server.go.common

import temple.ast.AttributeType
import temple.ast.AttributeType._
import temple.generate.server.go.GoHTTPStatus.GoHTTPStatus
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode, _}
import temple.generate.utils.{CodeTerm, CodeUtils}
import temple.utils.StringUtils.tabIndent

import scala.collection.immutable.ListMap

object GoCommonGenerator {

  private[go] def generateMod(module: String): String = mkCode.doubleLines(s"module $module", "go 1.13")

  private[go] def generatePackage(packageName: String): String = s"package $packageName"

  private[go] def generateGoType(attributeType: AttributeType): String =
    attributeType match {
      case ForeignKey(_) | UUIDType                       => "uuid.UUID"
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
      case DateType | TimeType | DateTimeType             => "time.Time"
      case BlobType(Some(size))                           => s"[$size]byte"
      case BlobType(_)                                    => "[]byte"
    }

  private[go] def genCheckAndReturnError(returnValues: String*): String =
    mkCode(
      "if err != nil",
      CodeWrap.curly.tabbed(
        mkCode(
          "return",
          mkCode.list(returnValues, "err"),
        ),
      ),
    )

  private[go] def genIfErr(body: String): String =
    mkCode(
      "if err != nil",
      CodeWrap.curly.tabbed(
        body,
      ),
    )

  /** Generate a constant */
  private[go] def genConst(identifier: String, expr: String): String =
    mkCode(
      "const",
      identifier,
      "=",
      expr,
    )

  /** Generate an interface */
  private[go] def genInterface(identifier: String, methods: Set[String]): String =
    mkCode(
      "type",
      identifier,
      "interface",
      CodeWrap.curly.tabbed(
        methods,
      ),
    )

  /** Generate a struct */
  private[go] def genStruct(identifier: String, fieldGroups: Iterable[(String, String)]*): String =
    mkCode(
      "type",
      identifier,
      "struct",
      CodeWrap.curly.tabbed(
        mkCode.doubleLines(fieldGroups.map { fields =>
          mkCode.lines(CodeUtils.pad(fields))
        }),
      ),
    )

  //** Generate a struct with annotations */
  private[go] def genStructWithAnnotations(identifier: String, fields: Iterable[(String, String, String)]): String =
    mkCode(
      "type",
      identifier,
      "struct",
      CodeWrap.curly.tabbed(
        CodeUtils.padThree(fields),
      ),
    )

  /** Generate a declaration and assignment statement */
  private[go] def genDeclareAndAssign(expr: String, identifiers: String*): String =
    mkCode.list(identifiers) + " := " + expr // mkCode doesn't put a space before the colon

  /** Generate an assignment */
  private[go] def genAssign(expr: String, identifiers: String*): String =
    mkCode(
      mkCode.list(identifiers),
      "=",
      expr,
    )

  /** Generate a variable declaration */
  private[go] def genVar(identifier: String, typ: String): String =
    mkCode(
      "var",
      identifier,
      typ,
    )

  /** Generate a function call */
  private[go] def genFunctionCall(name: String, args: CodeTerm*): String =
    CodeWrap.parens.prefix(name).list(args)

  /** Generate a function call, but with optional arguments */
  private[go] def genFunctionCall(name: String, firstArg: Option[String], otherArgs: Option[String]*): String =
    CodeWrap.parens.prefix(name).list(firstArg, otherArgs)

  /** Generate a method call */
  private[go] def genMethodCall(objectName: String, methodName: String, args: CodeTerm*): String =
    s"$objectName.${genFunctionCall(methodName, args: _*)}"

  /** Generate a method definition */
  private[go] def genMethod(
    objectName: String,
    objectType: String,
    methodName: String,
    methodArgs: Seq[String],
    methodReturn: Option[String],
    methodBody: String,
  ): String =
    mkCode(
      "func",
      CodeWrap.parens(objectName, objectType),
      CodeWrap.parens.prefix(methodName).list(methodArgs),
      methodReturn,
      CodeWrap.curly.tabbed(methodBody),
    )

  /** Generate a function */
  private[go] def genFunc(
    funcName: String,
    funcArgs: Seq[String],
    funcReturn: Option[String],
    funcBody: String,
  ): String =
    mkCode(
      "func",
      CodeWrap.parens.prefix(funcName).list(funcArgs),
      funcReturn,
      CodeWrap.curly.tabbed(funcBody),
    )

  /** Generate an anonymous goroutine */
  private[go] def genAnonGoroutine(
    args: Seq[String],
    body: String,
    invocationArgs: Seq[String],
  ): String =
    mkCode(
      "go",
      CodeWrap.parens.prefix("func").list(args),
      CodeWrap.parens.prefix(CodeWrap.curly.tabbed(body))(invocationArgs),
    )

  /** Generate an if statement */
  private[go] def genIf(expr: String, body: String): String =
    mkCode(
      "if",
      expr,
      CodeWrap.curly.tabbed(body),
    )

  /** Generate a for loop with expression */
  private[go] def genForLoop(expr: String, body: String): String =
    mkCode(
      "for",
      expr,
      CodeWrap.curly.tabbed(body),
    )

  /** Generate a switch statement */
  private[go] def genSwitch(expr: String, cases: ListMap[String, String], default: String): String =
    mkCode(
      "switch",
      expr,
      CodeWrap.curly.lines(
        cases.map { case (switchCase, statements) => mkCode.lines(s"case $switchCase:", tabIndent(statements)) },
        "default:",
        tabIndent(default),
      ),
    )

  /** Generate a switch statement and return */
  private[go] def genSwitchReturn(expr: String, cases: ListMap[String, String], default: String): String =
    mkCode.lines(
      genSwitch(expr, cases, default),
      genReturn(),
    )

  /** Generate a return statement */
  private[go] def genReturn(exprs: String*): String =
    mkCode(
      "return",
      mkCode.list(exprs),
    )

  /** Generate a populated struct */
  private[go] def genPopulateStruct(name: String, body: ListMap[String, String]): String =
    CodeWrap.curly
      .prefix(name)
      .tabbedTrailingList(CodeUtils.pad(body.map { case (k, v) => (k + ":", v) }))

  /** Generate a status code enumeration */
  private[go] def genHTTPEnum(statusCodeEnum: GoHTTPStatus): String =
    s"http.$statusCodeEnum"
}
