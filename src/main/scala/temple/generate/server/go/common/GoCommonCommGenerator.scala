package temple.generate.server.go.common

import temple.generate.server.go.common.GoCommonGenerator._
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.FileUtils

object GoCommonCommGenerator {

  private[go] def generateInit(): String =
    mkCode.lines(
      "// Init sets up the Handler object with a list of services from the config",
      mkCode(
        "func Init(config *util.Config) *Handler",
        CodeWrap.curly.tabbed(
          genReturn("&Handler{config.Services}"),
        ),
      ),
    )
  FileUtils.readResources("go/genFiles/common/comm/init.go.snippet").stripLineEnd
}
