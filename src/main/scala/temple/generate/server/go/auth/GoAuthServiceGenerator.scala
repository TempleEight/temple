package temple.generate.server.go.auth

import temple.generate.FileSystem._
import temple.generate.server.go.common._
import temple.generate.server.{AuthServiceGenerator, AuthServiceRoot}
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
        GoCommonMainGenerator.generateJsonMiddleware(),
        GoAuthServiceMainGenerator.generateHandlers(),
        GoAuthServiceMainGenerator.generateCreateToken(),
      ),
      File("auth/comm", "handler.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("comm"),
        GoAuthServiceCommGenerator.generateImports(authServiceRoot.module),
        GoAuthServiceCommGenerator.generateStructs(),
        GoCommonCommGenerator.generateInit(),
        GoAuthServiceCommGenerator.generateCreateJWTCredential(),
      ),
      File("auth/util", "util.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("util"),
        GoAuthServiceUtilGenerator.generateImports(),
        GoCommonUtilGenerator.generateConfigStruct(),
        GoCommonUtilGenerator.generateGetConfig(),
        GoCommonUtilGenerator.generateCreateErrorJSON(),
      ),
    ).map { case (path, contents) => path -> (contents + "\n") }
}