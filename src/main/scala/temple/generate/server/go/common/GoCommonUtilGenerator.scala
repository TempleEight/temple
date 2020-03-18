package temple.generate.server.go.common

import temple.utils.FileUtils

object GoCommonUtilGenerator {

  private[go] def generateConfigStruct(): String =
    FileUtils.readResources("go/genFiles/common/util/config_struct.go.snippet").stripLineEnd

  private[go] def generateGetConfig(): String =
    FileUtils.readResources("go/genFiles/common/util/get_config.go.snippet").stripLineEnd

  private[go] def generateCreateErrorJSON(): String =
    FileUtils.readResources("go/genFiles/common/util/create_error_json.go.snippet").stripLineEnd
}
