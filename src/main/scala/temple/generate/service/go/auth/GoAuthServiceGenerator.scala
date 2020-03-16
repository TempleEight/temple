package temple.generate.service.go.auth

import temple.generate.FileSystem._
import temple.generate.service.go.GoCommonGenerator
import temple.generate.service.{AuthServiceGenerator, AuthServiceRoot}
import temple.generate.utils.CodeTerm.mkCode

object GoAuthServiceGenerator extends AuthServiceGenerator {

  override def generate(authServiceRoot: AuthServiceRoot): Map[File, FileContent] =
    /* TODO
     * auth.go main
     * auth.go handlers
     */
    Map(
      File("auth", "go.mod") -> GoCommonGenerator.generateMod(authServiceRoot.module),
      File("auth", "auth.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoAuthServiceMainGenerator.generateImports(
          authServiceRoot.module,
        ),
        GoAuthServiceMainGenerator.generateStructs(),
        GoAuthServiceMainGenerator.generateRouter(),
        GoAuthServiceMainGenerator.generateMain(),
        GoCommonGenerator.generateJsonMiddleware(),
        GoAuthServiceMainGenerator.generateHandlers(),
        GoAuthServiceMainGenerator.generateCreateToken(),
      ),
    ).map { case (path, contents) => path -> (contents + "\n") }
}
