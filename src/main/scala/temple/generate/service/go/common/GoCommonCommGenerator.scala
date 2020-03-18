package temple.generate.service.go.common

import temple.utils.FileUtils

object GoCommonCommGenerator {

  private[go] def generateInit(): String =
    FileUtils.readResources("go/genFiles/common/comm/init.go.snippet").stripLineEnd
}
