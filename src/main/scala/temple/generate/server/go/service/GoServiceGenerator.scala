package temple.generate.server.go.service

import temple.generate.FileSystem._
import temple.generate.server.go.common._
import temple.generate.server.go.service.dao._
import temple.generate.server.{ServiceGenerator, ServiceRoot}
import temple.generate.utils.CodeTerm.mkCode

import scala.Option.when

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  override def generate(root: ServiceRoot): Files = {
    /* TODO
     * main in <>.go
     * handlers in <>.go
     * config.json
     */
    val usesComms  = root.comms.nonEmpty
    val operations = root.opQueries.keySet
    (Map(
      File(s"${root.name}", "go.mod") -> GoCommonGenerator.generateMod(root.module),
      File(root.name, s"${root.name}.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoServiceMainGenerator.generateImports(root, usesComms),
        GoServiceMainGenerator.generateGlobals(root, usesComms),
        GoServiceMainGenerator.generateMain(root, usesComms, operations),
        GoCommonMainGenerator.generateJsonMiddleware(),
        GoServiceMainGenerator.generateHandlers(root, operations),
      ),
      File(s"${root.name}/dao", "errors.go") -> GoServiceDAOGenerator.generateErrors(root),
      File(s"${root.name}/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoServiceDAOGenerator.generateImports(root),
        GoServiceDAOInterfaceGenerator.generateInterface(root, operations),
        GoCommonDAOGenerator.generateDAOStruct(),
        GoServiceDAOGenerator.generateDatastoreObjectStruct(root),
        GoServiceDAOInputStructsGenerator.generateStructs(root, operations),
        GoCommonDAOGenerator.generateInit(),
        GoServiceDAOGenerator.generateQueryFunctions(operations),
        GoServiceDAOFunctionsGenerator.generateDAOFunctions(root),
      ),
      File(s"${root.name}/util", "util.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("util"),
        GoServiceUtilGenerator.generateImports(),
        GoCommonUtilGenerator.generateConfigStruct(),
        GoServiceUtilGenerator.generateAuthStruct(),
        GoCommonUtilGenerator.generateGetConfig(),
        GoCommonUtilGenerator.generateCreateErrorJSON(),
        GoServiceUtilGenerator.generateIDsFromRequest(),
      ),
    ) ++ when(usesComms)(
      File(s"${root.name}/comm", "handler.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("comm"),
        GoServiceCommGenerator.generateImports(root),
        GoServiceCommGenerator.generateInterface(root),
        GoServiceCommGenerator.generateHandlerStruct(),
        GoCommonCommGenerator.generateInit(),
        GoServiceCommGenerator.generateCommFunctions(root),
      ),
    )).map { case (path, contents) => path -> (contents + "\n") }
  }
}
