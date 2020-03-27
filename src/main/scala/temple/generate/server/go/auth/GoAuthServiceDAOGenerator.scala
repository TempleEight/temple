package temple.generate.server.go.auth

import temple.generate.server.AuthServiceRoot
import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceDAOGenerator {

  private[auth] def generateErrors(root: AuthServiceRoot): String =
    mkCode.lines(
      generatePackage("dao"),
      "",
      s"import ${doubleQuote("errors")}",
      "",
      s"// ErrAuthNotFound is returned when the provided ${root.authType.name} was not found",
      s"var ErrAuthNotFound = errors.New(${doubleQuote("auth not found")})",
      "",
      "// ErrDuplicateAuth is returned when an auth already exists",
      s"var ErrDuplicateAuth = errors.New(${doubleQuote("auth already exists")})",
    )
}
