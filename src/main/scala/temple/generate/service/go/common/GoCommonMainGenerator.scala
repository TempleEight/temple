package temple.generate.service.go.common

import temple.utils.FileUtils

object GoCommonMainGenerator {

  private[go] def generateJsonMiddleware(): String =
    FileUtils.readResources("go/genFiles/json_middleware.go.snippet").stripLineEnd
}
