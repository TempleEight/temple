package temple.generate.service.go.auth

import temple.generate.service.{AuthServiceGenerator, AuthServiceRoot}
import temple.generate.service.go.GoCommonGenerator
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.generate.FileSystem._
import temple.utils.StringUtils.doubleQuote

object GoAuthServiceGenerator extends AuthServiceGenerator {

  override def generate(authServiceRoot: AuthServiceRoot): Map[File, FileContent] =
    (
      Map(
        File("auth", "go.mod") -> GoCommonGenerator.generateMod(authServiceRoot.module),
        File("auth", "auth.go") -> mkCode.doubleLines(
          GoCommonGenerator.generatePackage("main"),
          GoAuthServiceMainGenerator.generateAuthImports(
            authServiceRoot.module,
          ),
        ),
      ),
    ).map { case (path, contents) => path -> (contents + "\n") }
}
