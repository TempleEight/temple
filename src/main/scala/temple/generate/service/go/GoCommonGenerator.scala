package temple.generate.service.go

import temple.generate.utils.CodeTerm.mkCode

object GoCommonGenerator {

  private[go] def generateMod(module: String): String = mkCode.doubleLines(s"module $module", "go 1.13")

  private[go] def generatePackage(packageName: String): String = s"package $packageName"
}
