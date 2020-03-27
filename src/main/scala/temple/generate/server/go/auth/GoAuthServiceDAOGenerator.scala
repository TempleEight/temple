package temple.generate.server.go.auth

import temple.generate.server.AuthServiceRoot
import temple.generate.server.go.common.GoCommonDAOGenerator
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.utils.CodeUtils
import temple.utils.StringUtils.doubleQuote

import scala.collection.immutable.ListMap

object GoAuthServiceDAOGenerator {

  private[auth] def generateImports(root: AuthServiceRoot): String =
    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        doubleQuote("database/sql"),
        doubleQuote("fmt"),
        "",
        doubleQuote(s"${root.module}/util"),
        doubleQuote("github.com/google/uuid"),
        "",
        "// pq acts as the driver for SQL requests",
        doubleQuote("github.com/lib/pq"),
      ),
    )

  private[auth] def generateGlobals(): String =
    mkCode.lines(
      "// https://www.postgresql.org/docs/9.3/errcodes-appendix.html",
      genConst("psqlUniqueViolation", doubleQuote("unique_violation")),
    )

  private[auth] def generateInterface(): String =
    mkCode.lines(
      "// Datastore provides the interface adopted by the DAO, allowing for mocking",
      mkCode(
        "type Datastore interface",
        CodeWrap.curly.tabbed(
          "CreateAuth(input CreateAuthInput) (*Auth, error)",
          "ReadAuth(input ReadAuthInput) (*Auth, error)",
        ),
      ),
    )

  private[auth] def generateStructs(root: AuthServiceRoot): String = {
    val id       = ListMap(root.idAttribute.name.toUpperCase           -> generateGoType(root.idAttribute.attributeType))
    val auth     = ListMap(root.authAttribute.authType.name.capitalize -> generateGoType(root.authAttribute.attributeType))
    val password = ListMap("Password"                                  -> "string")
    mkCode.lines(
      "// Auth encapsulates the object stored in the datastore",
      genStruct("Auth", id ++ auth ++ password),
      "",
      "// CreateAuthInput encapsulates the information required to create a single auth in the datastore",
      genStruct("CreateAuthInput", id ++ auth ++ password),
      "",
      "// ReadAuthInput encapsulates the information required to read a single auth in the datastore",
      genStruct("ReadAuthInput", auth),
    )
  }

  private[auth] def generateQueryFunctions(): String =
    mkCode.doubleLines(
      GoCommonDAOGenerator.generateExecuteQueryWithRowResponse(),
      GoCommonDAOGenerator.generateExecuteQuery(),
    )

  private[auth] def generateErrors(root: AuthServiceRoot): String =
    mkCode.lines(
      generatePackage("dao"),
      "",
      s"import ${doubleQuote("errors")}",
      "",
      s"// ErrAuthNotFound is returned when the provided ${root.authAttribute.authType.name} was not found",
      s"var ErrAuthNotFound = errors.New(${doubleQuote("auth not found")})",
      "",
      "// ErrDuplicateAuth is returned when an auth already exists",
      s"var ErrDuplicateAuth = errors.New(${doubleQuote("auth already exists")})",
    )
}
