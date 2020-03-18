package temple.generate.server.go.service

import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils

object GoServiceDaoGenerator {

  private[go] def generateImports(module: String): String = mkCode(
    "import",
    CodeWrap.parens.tabbed(
      """"database/sql"""",
      """"fmt"""",
      "",
      s""""$module/util"""",
      "// pq acts as the driver for SQL requests",
      """_ "github.com/lib/pq"""",
    ),
  )

  private[go] def generateStructs(): String =
    mkCode.lines(
      "// DAO encapsulates access to the database",
      s"type DAO struct ${CodeWrap.curly.tabbed("DB *sql.DB")}",
    )

  private[go] def generateInit(): String =
    FileUtils.readResources("go/genFiles/common/dao/init.go.snippet").stripLineEnd

  private[go] def generateErrors(serviceName: String): String =
    mkCode.lines(
      "package dao",
      "",
      """import "fmt"""",
      "",
      s"// Err${serviceName.capitalize}NotFound is returned when a $serviceName for the provided ID was not found",
      s"type Err${serviceName.capitalize}NotFound int64",
      "",
      s"func (e Err${serviceName.capitalize}NotFound) Error() string ${CodeWrap.curly
        .tabbed(s"""return fmt.Sprintf("$serviceName not found with ID %d", e)""")}",
    )
}
