package temple.generate.server.go.service

import temple.ast.Annotation.Unique
import temple.ast.AttributeType
import temple.ast.Metadata.{Readable, Writable}
import temple.generate.CRUD
import temple.generate.FileSystem._
import temple.generate.server.AttributesRoot.ServiceRoot
import temple.generate.server.ServiceGenerator
import temple.generate.server.go.common._
import temple.generate.server.go.service.dao._
import temple.generate.server.go.service.main.{GoServiceMainGenerator, GoServiceMainHandlersGenerator, GoServiceMainStructGenerator}
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
        .intersect(root.blockIterator.flatMap(_.attributes.values).map(_.attributeType).toSet)
        .nonEmpty

    // Whether or not this service uses base64, by checking for attributes of type blob
    val usesBase64 =
      root.blockIterator.flatMap(_.attributes.values).exists(_.attributeType.isInstanceOf[AttributeType.BlobType])

    // Whether or not the service uses metrics
    val usesMetrics = root.metrics.isDefined

    (Map(
      File(s"${root.kebabName}", "go.mod") -> GoCommonGenerator.generateMod(root.module),
      File(root.kebabName, s"${root.kebabName}.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoServiceMainGenerator.generateImports(root, usesBase64, usesTime, usesComms, usesMetrics),
        GoServiceMainStructGenerator.generateEnvStruct(usesComms),
        root.blockIterator.map { block =>
          when(
            block.requestAttributes.nonEmpty
            && (block.opQueries.contains(CRUD.Create) || block.opQueries.contains(CRUD.Update)),
          ) {
            GoServiceMainStructGenerator.generateRequestStructs(block)
          }
        },
        root.blockIterator.map { block =>
          GoServiceMainStructGenerator.generateResponseStructs(block)
        },
        GoServiceMainGenerator.generateRouter(root),
        GoCommonMainGenerator.generateMain(root, usesComms, isAuth = false, usesMetrics),
        GoCommonMainGenerator.generateJsonMiddleware(),
        when(
          (root.readable == Readable.This || root.writable == Writable.This) && (!root.hasAuthBlock || root.structs.nonEmpty),
        ) {
          GoServiceMainGenerator.generateCheckAuthorization(root)
        },
        root.structs.map(GoServiceMainGenerator.generateCheckParent),
        GoCommonMainGenerator.generateRespondWithErrorFunc(usesMetrics),
        root.blockIterator.map { block =>
          GoServiceMainHandlersGenerator.generateHandlers(block, when(root != block)(root), usesComms, usesMetrics)
        },
      ),
      File(root.kebabName, "setup.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoCommonSetupGenerator.generateImports,
        GoCommonSetupGenerator.generateSetupMethod,
      ),
      File(root.kebabName, "hook.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("main"),
        GoServiceHookGenerator.generateImports(root),
        GoServiceHookGenerator.generateHookStruct(root),
        GoCommonHookGenerator.generateHookErrorStruct,
        GoCommonHookGenerator.generateHookErrorFunction,
        GoServiceHookGenerator.generateAddHookMethods(root),
      ),
      File(s"${root.kebabName}/dao", "errors.go") -> GoServiceDAOGenerator.generateErrors(root),
      File(s"${root.kebabName}/dao", "dao.go") -> mkCode.doubleLines(
        GoCommonGenerator.generatePackage("dao"),
        GoServiceDAOGenerator.generateImports(root, usesTime),
        GoServiceDAOInterfaceGenerator.generateInterface(root),
        GoCommonDAOGenerator.generateDAOStruct(),
        root.blockIterator.map { block =>
          GoServiceDAOGenerator.generateDatastoreObjectStruct(block)
        },
        root.blockIterator.map { block =>
          GoServiceDAOInputStructsGenerator.generateStructs(block)
        },
        when(root.contains(Unique)) { GoServiceDAOGenerator.generateUniqueConstant() },
        GoCommonDAOGenerator.generateInit(),
        GoServiceDAOGenerator.generateQueryFunctions(root.blockIterator.flatMap(_.operations).toSet),
        root.blockIterator.map { block =>
          GoServiceDAOFunctionsGenerator.generateDAOFunctions(block)
        },
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
        GoServiceUtilGenerator.generateIDsFromRequest(root.structs.nonEmpty),
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
        GoServiceMetricGenerator.generateVars(root),
      ),
    )).map { case (path, contents) => path -> (contents + "\n") }
  }
}
