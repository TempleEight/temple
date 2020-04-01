package temple.generate.server.go.service

import temple.ast.{Annotation, AttributeType}
import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.server.go.common._
import temple.generate.server.go.service.dao._
import temple.generate.server.go.service.main.{GoServiceMainGenerator, GoServiceMainStructGenerator}
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

    // Set of CRUD operations the service uses
    val operations = root.opQueries.keySet

    // Whether or not the service uses inter-service communication
    val usesComms = root.comms.nonEmpty

    // Whether or not ths service uses the time type, by checking for attributes of type date, time or datetime
    val usesTime =
      Set[AttributeType](AttributeType.DateType, AttributeType.TimeType, AttributeType.DateTimeType)
        .intersect(root.attributes.values.map(_.attributeType).toSet)
        .nonEmpty

    // Attributes filtered by which are client-provided
    val clientAttributes = root.attributes.filterNot {
      case (_, attr) =>
        attr.accessAnnotation.contains(Annotation.Server) || attr.accessAnnotation.contains(Annotation.ServerSet)
    }

    (Map(
      File(s"${root.name}", "go.mod") -> GoCommonGenerator.generateMod(root.module),
      File(root.name, s"${root.name}.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoServiceMainGenerator.generateImports(root, usesTime, usesComms, clientAttributes),
        GoServiceMainStructGenerator.generateEnvStruct(usesComms),
        when(clientAttributes.nonEmpty && (operations.contains(CRUD.Create) || operations.contains(CRUD.Read))) {
          GoServiceMainStructGenerator.generateRequestStructs(root, operations, clientAttributes)
        },
        GoServiceMainStructGenerator.generateResponseStructs(root, operations),
        GoServiceMainGenerator.generateRouter(root, operations),
        GoServiceMainGenerator.generateMain(root, usesComms, operations),
        GoCommonMainGenerator.generateJsonMiddleware(),
        GoServiceMainGenerator.generateHandlers(root, operations),
      ),
      File(s"${root.name}/dao", "errors.go") -> GoServiceDAOGenerator.generateErrors(root),
      File(s"${root.name}/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoServiceDAOGenerator.generateImports(root, usesTime),
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
      File(s"${root.name}/metric", "metric.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("metric"),
        GoServiceMetricGenerator.generateImports(),
        GoServiceMetricGenerator.generateVars(root, operations),
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
