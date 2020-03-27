package temple.generate.server.go.auth

import temple.generate.server.AuthServiceRoot
import temple.generate.server.go.common.GoCommonDAOGenerator
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.{doubleQuote, tabIndent}

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

  private def generateCreateFunction(root: AuthServiceRoot, scanFunctionCall: String): String = {
    val queryStatement = genDeclareAndAssign(
      genFunctionCall(
        "executeQueryWithRowResponse",
        "dao.DB",
        doubleQuote(root.createQuery),
        s"input.${root.idAttribute.name.toUpperCase()}",
        s"input.${root.authAttribute.authType.name.capitalize}",
        s"input.Password",
      ),
      "row",
    )

    val scanBlock = mkCode.lines(
      genVar("auth", "Auth"),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      genIf(
        "err != nil",
        mkCode.lines(
          "// PQ specific error",
          genIf(
            "err, ok := err.(*pq.Error); ok",
            mkCode.lines(
              genIf("err.Code.Name() == psqlUniqueViolation", "return nil, ErrDuplicateAuth"),
            ),
          ),
          genReturn("nil", "err"),
        ),
      ),
    )

    mkCode.lines(
      "// CreateAuth creates a new auth in the datastore, returning the newly created auth",
      mkCode(
        "func (dao *DAO) CreateAuth(input CreateAuthInput) (*Auth, error)",
        CodeWrap.curly.tabbed(
          mkCode.doubleLines(
            queryStatement,
            scanBlock,
            genReturn("&auth", "nil"),
          ),
        ),
      ),
    )
  }

  private def generateReadFunction(root: AuthServiceRoot, scanFunctionCall: String): String = {
    val queryStatement = genDeclareAndAssign(
      genFunctionCall(
        "executeQueryWithRowResponse",
        "dao.DB",
        doubleQuote(root.readQuery),
        s"input.${root.authAttribute.authType.name.capitalize}",
      ),
      "row",
    )

    val scanBlock = mkCode.lines(
      genVar("auth", "Auth"),
      genDeclareAndAssign(s"row.$scanFunctionCall", "err"),
      genIf(
        "err != nil",
        mkCode.lines(
          mkCode(
            "switch err",
            CodeWrap.curly.noIndent(
              "case sql.ErrNoRows:",
              tabIndent(genReturn("nil", "ErrAuthNotFound")),
              "default:",
              tabIndent(genReturn("nil", "err")),
            ),
          ),
        ),
      ),
    )

    mkCode.lines(
      s"// ReadAuth returns the auth in the datastore for a given ${root.authAttribute.authType.name}",
      mkCode(
        "func (dao *DAO) ReadAuth(input ReadAuthInput) (*Auth, error)",
        CodeWrap.curly.tabbed(
          mkCode.doubleLines(
            queryStatement,
            scanBlock,
            genReturn("&auth", "nil"),
          ),
        ),
      ),
    )
  }

  private[auth] def generateDAOFunctions(root: AuthServiceRoot): String = {
    val scanFunctionCall = genFunctionCall(
      "Scan",
      s"&auth.${root.idAttribute.name.toUpperCase()}",
      s"&auth.${root.authAttribute.authType.name.capitalize}",
      s"&auth.Password",
    )
    mkCode.doubleLines(
      generateCreateFunction(root, scanFunctionCall),
      generateReadFunction(root, scanFunctionCall),
    )
  }

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
