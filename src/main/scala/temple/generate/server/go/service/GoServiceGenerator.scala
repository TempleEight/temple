package temple.generate.server.go.service

import temple.generate.FileSystem._
import temple.generate.server.go.common._
import temple.generate.server.{ServiceGenerator, ServiceRoot}
import temple.generate.utils.CodeTerm.mkCode

import scala.Option.when

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  override def generate(serviceRoot: ServiceRoot): Files = {
    /* TODO
     * main in <>.go
     * handlers in <>.go
     * dao.go
     * handler.go
     * config.json
     */
    val usesComms = serviceRoot.comms.nonEmpty
    (Map(
      File(s"${serviceRoot.name}", "go.mod") -> GoCommonGenerator.generateMod(serviceRoot.module),
      File(serviceRoot.name, s"${serviceRoot.name}.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoServiceMainGenerator.generateImports(
          serviceRoot.name,
          serviceRoot.module,
          usesComms,
        ),
        GoServiceMainGenerator.generateGlobals(
          serviceRoot.name,
          usesComms,
        ),
        GoServiceMainGenerator.generateMain(
          serviceRoot.name,
          usesComms,
          serviceRoot.operations,
          serviceRoot.port,
        ),
        GoCommonMainGenerator.generateJsonMiddleware(),
        GoServiceMainGenerator.generateHandlers(
          serviceRoot.name,
          serviceRoot.operations,
        ),
      ),
      File(s"${serviceRoot.name}/dao", "errors.go") -> GoServiceDAOGenerator.generateErrors(serviceRoot.name),
      File(s"${serviceRoot.name}/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoServiceDAOGenerator.generateImports(
          serviceRoot.attributes,
          serviceRoot.module,
        ),
        GoServiceDAOGenerator.generateDatastoreInterface(
          serviceRoot.name,
          serviceRoot.operations,
          serviceRoot.createdByAttribute,
        ),
        GoCommonDAOGenerator.generateDAOStruct(),
        GoServiceDAOGenerator.generateDatastoreObjectStruct(
          serviceRoot.name,
          serviceRoot.idAttribute,
          serviceRoot.createdByAttribute,
          serviceRoot.attributes,
        ),
        GoServiceDAOGenerator.generateInputStructs(
          serviceRoot.name,
          serviceRoot.operations,
          serviceRoot.idAttribute,
          serviceRoot.createdByAttribute,
          serviceRoot.attributes,
        ),
      ),
      File(s"${serviceRoot.name}/util", "util.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("util"),
        GoServiceUtilGenerator.generateImports(),
        GoCommonUtilGenerator.generateConfigStruct(),
        GoServiceUtilGenerator.generateAuthStruct(),
        GoCommonUtilGenerator.generateGetConfig(),
        GoCommonUtilGenerator.generateCreateErrorJSON(),
        GoServiceUtilGenerator.generateIDsFromRequest(),
      ),
    ) ++ when(usesComms)(
      File(s"${serviceRoot.name}/comm", "handler.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("comm"),
        GoServiceCommGenerator.generateImports(serviceRoot.module),
        GoServiceCommGenerator.generateStructs(),
        GoCommonCommGenerator.generateInit(),
      ),
    )).map { case (path, contents) => path -> (contents + "\n") }
  }
}
