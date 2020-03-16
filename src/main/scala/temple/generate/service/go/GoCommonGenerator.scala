package temple.generate.service.go

import temple.generate.utils.CodeTerm.mkCode
import temple.utils.FileUtils

object GoCommonGenerator {

  private[go] def generateMod(module: String): String = mkCode.doubleLines(s"module $module", "go 1.13")

  private[go] def generatePackage(packageName: String): String = s"package $packageName"

  private[go] def generateJsonMiddleware(): String =
    FileUtils.readResources("go/genFiles/json_middleware.go").stripLineEnd

  private[go] def generateCommInit(): String =
    FileUtils.readResources("go/genFiles/comm_init.go").stripLineEnd
}
