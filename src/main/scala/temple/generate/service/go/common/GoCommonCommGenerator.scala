package temple.generate.service.go.common

import temple.utils.FileUtils

object GoCommonCommGenerator {

  private[go] def generateInit(): String =
    FileUtils.readResources("go/genFiles/common/comm_init.go").stripLineEnd
}
