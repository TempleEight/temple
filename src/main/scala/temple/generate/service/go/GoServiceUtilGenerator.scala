package temple.generate.service.go

import temple.utils.FileUtils

object GoServiceUtilGenerator {

  private[go] def generateConfig(): String =
    FileUtils.readResources("go/genFiles/config.go").stripLineEnd

  private[go] def generateUtil(): String =
    FileUtils.readResources("go/genFiles/util.go").stripLineEnd
}
