package temple.generate.service.go.common

import temple.utils.FileUtils

object GoCommonMainGenerator {

  private[go] def generateJsonMiddleware(): String =
    FileUtils.readResources("go/genFiles/common/main/json_middleware.go.snippet").stripLineEnd
}
