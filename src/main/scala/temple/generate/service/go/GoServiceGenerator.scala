package temple.generate.service.go

import temple.generate.FileSystem._
import temple.generate.service.{ServiceGenerator, ServiceRoot}
import temple.generate.utils.CodeTerm.mkCode

import scala.Option.when

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  override def generate(serviceRoot: ServiceRoot): Map[File, FileContent] = {
    /* TODO
     * handlers in <>.go
     * structs and methods in dao.go
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
          serviceRoot.endpoints,
          serviceRoot.port,
        ),
        GoServiceMainGenerator.generateJsonMiddleware(),
        GoServiceMainGenerator.generateHandlers(
          serviceRoot.name,
          serviceRoot.endpoints,
        ),
      ),
      File(s"${serviceRoot.name}/dao", "errors.go") -> GoServiceDaoGenerator.generateErrors(serviceRoot.name),
      File(s"${serviceRoot.name}/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoServiceDaoGenerator.generateImports(serviceRoot.module),
        GoServiceDaoGenerator.generateStructs(),
        GoServiceDaoGenerator.generateInit(),
      ),
      File(s"${serviceRoot.name}/util", "config.go") -> GoServiceUtilGenerator.generateConfig(),
      File(s"${serviceRoot.name}/util", "util.go")   -> GoServiceUtilGenerator.generateUtil(),
    ) ++ when(usesComms)(
      File(s"${serviceRoot.name}/comm", "handler.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("comm"),
        GoServiceCommGenerator.generateImports(serviceRoot.module),
        GoServiceCommGenerator.generateStructs(),
        GoServiceCommGenerator.generateInit(),
      ),
    )).map { case (path, contents) => path -> (contents + "\n") }
  }
}
