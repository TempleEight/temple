package temple.generate.service.go.auth

import temple.generate.FileSystem._
import temple.generate.service.go.common._
import temple.generate.service.{AuthServiceGenerator, AuthServiceRoot}
import temple.generate.utils.CodeTerm.mkCode

object GoAuthServiceGenerator extends AuthServiceGenerator {

  override def generate(authServiceRoot: AuthServiceRoot): Map[File, FileContent] =
    /* TODO
     * auth.go main
     * auth.go handlers
     * handler.go init
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
        GoCommonMainGenerator.generateJsonMiddleware(),
        GoAuthServiceMainGenerator.generateHandlers(),
        GoAuthServiceMainGenerator.generateCreateToken(),
      ),
      File("auth/comm", "handler.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("comm"),
        GoAuthServiceCommGenerator.generateImports(authServiceRoot.module),
        GoAuthServiceCommGenerator.generateStructs(),
        //GoCommonGenerator.generateCommInit(),
        GoAuthServiceCommGenerator.generateCreateJWTCredential(),
      ),
    ).map { case (path, contents) => path -> (contents + "\n") }
}
