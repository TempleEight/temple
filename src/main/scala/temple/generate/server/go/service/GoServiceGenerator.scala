package temple.generate.server.go.service

import temple.ast.AttributeType
import temple.ast.Metadata.{Readable, Writable}
import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.server.go.common._
import temple.generate.server.go.service.dao._
import temple.generate.server.go.service.main.{GoServiceMainGenerator, GoServiceMainHandlersGenerator, GoServiceMainStructGenerator}
import temple.generate.server.{ServiceGenerator, ServiceRoot}
import temple.generate.utils.CodeTerm.mkCode

import scala.Option.when

/** Implementation of [[ServiceGenerator]] for generating Go */
object GoServiceGenerator extends ServiceGenerator {

  override def generate(root: ServiceRoot): Files = {
    // Set of CRUD operations the service uses
    val operations = root.opQueries.keySet

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

    // Attributes filtered by which are client-provided
    val clientAttributes = root.attributes.filter { case (_, attr) => attr.inRequest }

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
          .generateImports(root, usesBase64, usesTime, usesComms, usesMetrics, clientAttributes, operations),
        GoServiceMainStructGenerator.generateEnvStruct(usesComms),
        when(clientAttributes.nonEmpty && (operations.contains(CRUD.Create) || operations.contains(CRUD.Update))) {
          GoServiceMainStructGenerator.generateRequestStructs(root, operations, clientAttributes)
        },
        GoServiceMainStructGenerator.generateResponseStructs(root, operations),
        GoServiceMainGenerator.generateRouter(root, operations),
        GoCommonMainGenerator.generateMain(root, root.port, usesComms, isAuth = false, usesMetrics),
        GoCommonMainGenerator.generateJsonMiddleware(),
        when((root.readable == Readable.This || root.writable == Writable.This) && !root.hasAuthBlock) {
          GoServiceMainGenerator.generateCheckAuthorization(root)
        },
        GoCommonMainGenerator.generateRespondWithErrorFunc(usesMetrics),
        GoServiceMainHandlersGenerator
          .generateHandlers(root, operations, clientAttributes, usesComms, enumeratingByCreator, usesMetrics),
      ),
      File(root.kebabName, "setup.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoCommonSetupGenerator.generateImports,
        GoCommonSetupGenerator.generateSetupMethod,
      ),
      File(root.kebabName, "hook.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoCommonHookGenerator.generateImports(root.module),
        GoServiceHookGenerator.generateHookStruct(root, clientAttributes, operations),
        GoCommonHookGenerator.generateHookErrorStruct,
        GoCommonHookGenerator.generateHookErrorFunction,
        GoServiceHookGenerator.generateAddHookMethods(root, clientAttributes, operations),
      ),
      File(s"${root.kebabName}/dao", "errors.go") -> GoServiceDAOGenerator.generateErrors(root),
      File(s"${root.kebabName}/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoServiceDAOGenerator.generateImports(root, usesTime),
        GoServiceDAOInterfaceGenerator.generateInterface(root, operations, enumeratingByCreator),
        GoCommonDAOGenerator.generateDAOStruct(),
        GoServiceDAOGenerator.generateDatastoreObjectStruct(root),
        GoServiceDAOInputStructsGenerator.generateStructs(root, operations, enumeratingByCreator),
        GoCommonDAOGenerator.generateInit(),
        GoServiceDAOGenerator.generateQueryFunctions(operations),
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
        GoServiceMetricGenerator.generateVars(root, operations),
      ),
    )).map { case (path, contents) => path -> (contents + "\n") }
  }
}
