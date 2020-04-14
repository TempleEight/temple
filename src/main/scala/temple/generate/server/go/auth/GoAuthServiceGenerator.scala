package temple.generate.server.go.auth

import temple.generate.FileSystem._
import temple.generate.server.go.common._
import temple.generate.server.{AuthServiceGenerator, AuthServiceRoot, ServiceName, ServiceRoot}
import temple.generate.utils.CodeTerm.mkCode

object GoAuthServiceGenerator extends AuthServiceGenerator {

  override def generate(root: AuthServiceRoot): Files =
    /* TODO
     * config.json
     */
    Map(
      File("auth", "go.mod") -> GoCommonGenerator.generateMod(root.module),
      File("auth", "auth.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoAuthServiceMainGenerator.generateImports(root),
        GoAuthServiceMainGenerator.generateStructs(),
        GoAuthServiceMainGenerator.generateRouter(),
        GoCommonMainGenerator.generateMain(ServiceName("Auth"), root.port, usesComms = true, isAuth = true),
        GoCommonMainGenerator.generateJsonMiddleware(),
        GoAuthServiceMainGenerator.generateHandlers(),
        GoAuthServiceMainGenerator.generateCreateToken(),
      ),
      File("auth", "setup.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoCommonSetupGenerator.generateImports,
        GoCommonSetupGenerator.generateSetupMethod,
      ),
      File("auth", "hook.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoCommonHookGenerator.generateImports(root.module),
        GoAuthServiceHookGenerator.generateHookStruct,
        GoCommonHookGenerator.generateHookErrorStruct,
        GoCommonHookGenerator.generateHookErrorFunction,
        GoAuthServiceHookGenerator.generateAddHookMethods,
      ),
      File("auth/dao", "errors.go") -> GoAuthServiceDAOGenerator.generateErrors(root),
      File("auth/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoAuthServiceDAOGenerator.generateImports(root),
        GoAuthServiceDAOGenerator.generateGlobals(),
        GoAuthServiceDAOGenerator.generateInterface(),
        GoCommonDAOGenerator.generateDAOStruct(),
        GoAuthServiceDAOGenerator.generateStructs(root),
        GoCommonDAOGenerator.generateInit(),
        GoAuthServiceDAOGenerator.generateQueryFunctions(),
        GoAuthServiceDAOGenerator.generateDAOFunctions(root),
      ),
      File("auth/dao", "datastore.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoCommonDAOGenerator.generateExtendableDatastoreInterface(),
      ),
      File("auth/comm", "handler.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("comm"),
        GoAuthServiceCommGenerator.generateImports(root),
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
      File("auth/metric", "metric.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("metric"),
        GoCommonMetricGenerator.generateImports(),
        GoAuthServiceMetricGenerator.generateVars(),
      ),
    ).map { case (path, contents) => path -> (contents + "\n") }
}
