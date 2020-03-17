package temple.generate.service.go

import temple.utils.FileUtils

object GoServiceUtilGenerator {

  private[go] def generateConfig(): String =
    FileUtils.readResources("go/genFiles/common/config.go.snippet").stripLineEnd

  private[go] def generateUtil(): String =
    FileUtils.readResources("go/genFiles/util.go.snippet").stripLineEnd
}
