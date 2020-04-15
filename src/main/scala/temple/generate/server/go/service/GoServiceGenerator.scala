package temple.generate.server.go.service

import temple.ast.AttributeType
import temple.ast.Metadata.{Readable, Writable}
import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.go.common._
import temple.generate.server.go.service.dao._
import temple.generate.server.go.service.main.{GoServiceMainGenerator, GoServiceMainHandlersGenerator, GoServiceMainStructGenerator}
import temple.generate.server.ServiceGenerator
import temple.generate.utils.CodeTerm.mkCode

import scala.Option.when

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  override def generate(root: ServiceRoot): Files = {
    // Whether or not the service uses inter-service communication
    val usesComms = root.comms.nonEmpty

    // TODO: what if these attributes are serverSet, will time and base64 still be used?
    // Whether or not this service uses the time type, by checking for attributes of type date, time or datetime
    val usesTime =
      Set[AttributeType](AttributeType.DateType, AttributeType.TimeType, AttributeType.DateTimeType)
        .intersect(root.attributes.values.map(_.attributeType).toSet)
        .nonEmpty

    // Whether or not this service uses base64, by checking for attributes of type blob
    val usesBase64 = root.attributes.values.map(_.attributeType).toSet.contains(AttributeType.BlobType())

    // Whether or not the service uses metrics
    val usesMetrics = root.metrics.isDefined

    // Whether or not this service is enumerating by creator
    lazy val enumeratingByCreator = root.readable match {
      case Readable.All  => false
      case Readable.This => true
    }

    (Map(
      File(s"${root.kebabName}", "go.mod") -> GoCommonGenerator.generateMod(root.module),
      File(root.kebabName, s"${root.kebabName}.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoServiceMainGenerator
          .generateImports(root, usesBase64, usesTime, usesComms, usesMetrics, root.requestAttributes, root.operations),
        GoServiceMainStructGenerator.generateEnvStruct(usesComms),
        when(
          root.requestAttributes.nonEmpty
          && (root.operations.contains(CRUD.Create) || root.operations.contains(CRUD.Update)),
        ) {
          GoServiceMainStructGenerator.generateRequestStructs(root, root.operations, root.requestAttributes)
        },
        GoServiceMainStructGenerator.generateResponseStructs(root, root.operations),
        GoServiceMainGenerator.generateRouter(root, root.operations),
        GoCommonMainGenerator.generateMain(root, root.port, usesComms, isAuth = false, usesMetrics),
        GoCommonMainGenerator.generateJsonMiddleware(),
        when((root.readable == Readable.This || root.writable == Writable.This) && !root.hasAuthBlock) {
          GoServiceMainGenerator.generateCheckAuthorization(root)
        },
        GoServiceMainHandlersGenerator.generateHandlers(
          root,
          root.operations,
          root.requestAttributes,
          usesComms,
          enumeratingByCreator,
          usesMetrics,
        ),
      ),
      File(root.kebabName, "setup.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoCommonSetupGenerator.generateImports,
        GoCommonSetupGenerator.generateSetupMethod,
      ),
      File(root.kebabName, "hook.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoCommonHookGenerator.generateImports(root.module),
        GoServiceHookGenerator.generateHookStruct(root, root.requestAttributes, root.operations),
        GoCommonHookGenerator.generateHookErrorStruct,
        GoCommonHookGenerator.generateHookErrorFunction,
        GoServiceHookGenerator.generateAddHookMethods(root, root.requestAttributes, root.operations),
      ),
      File(s"${root.kebabName}/dao", "errors.go") -> GoServiceDAOGenerator.generateErrors(root),
      File(s"${root.kebabName}/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoServiceDAOGenerator.generateImports(root, usesTime),
        GoServiceDAOInterfaceGenerator.generateInterface(root, root.operations, enumeratingByCreator),
        GoCommonDAOGenerator.generateDAOStruct(),
        GoServiceDAOGenerator.generateDatastoreObjectStruct(root),
        GoServiceDAOInputStructsGenerator.generateStructs(root, root.operations, enumeratingByCreator),
        GoCommonDAOGenerator.generateInit(),
        GoServiceDAOGenerator.generateQueryFunctions(root.operations),
        GoServiceDAOFunctionsGenerator.generateDAOFunctions(root, enumeratingByCreator),
      ),
      File(s"${root.kebabName}/dao", "datastore.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoCommonDAOGenerator.generateExtendableDatastoreInterface(),
      ),
      File(s"${root.kebabName}/util", "util.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("util"),
        GoServiceUtilGenerator.generateImports(),
        GoCommonUtilGenerator.generateConfigStruct(),
        GoServiceUtilGenerator.generateAuthStruct(),
        GoCommonUtilGenerator.generateGetConfig(),
        GoCommonUtilGenerator.generateCreateErrorJSON(),
        GoServiceUtilGenerator.generateIDsFromRequest(),
      ),
    ) ++ when(usesComms)(
      File(s"${root.kebabName}/comm", "handler.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("comm"),
        GoServiceCommGenerator.generateImports(root),
        GoServiceCommGenerator.generateInterface(root),
        GoServiceCommGenerator.generateHandlerStruct(),
        GoCommonCommGenerator.generateInit(),
        GoServiceCommGenerator.generateCommFunctions(root),
      ),
    ) ++ when(usesMetrics)(
      File(s"${root.kebabName}/metric", "metric.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("metric"),
        GoCommonMetricGenerator.generateImports(),
        GoServiceMetricGenerator.generateVars(root, root.operations),
      ),
    )).map { case (path, contents) => path -> (contents + "\n") }
  }
}
