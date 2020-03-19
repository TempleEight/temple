package temple.generate.server.go.common

import temple.generate.utils.CodeTerm.mkCode
import temple.generate.utils.CodeTerm.CodeWrap

object GoCommonDAOGenerator {

  private[go] def generateDAOStruct(): String =
    mkCode.lines(
      "// DAO encapsulates access to the datastore",
      mkCode("type DAO struct", CodeWrap.curly.tabbed("DB *sql.DB")),
    )
}
