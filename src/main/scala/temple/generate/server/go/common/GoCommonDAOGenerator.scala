package temple.generate.server.go.common

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils

object GoCommonDAOGenerator {

  private[go] def generateDAOStruct(): String =
    mkCode.lines(
      "// DAO encapsulates access to the datastore",
      mkCode("type DAO struct", CodeWrap.curly.tabbed("DB *sql.DB")),
    )

  private[go] def generateInit(): String =
    FileUtils.readResources("go/genFiles/common/dao/init.go.snippet").stripLineEnd
}
