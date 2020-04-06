package temple.builder.project

import temple.ast.AbstractServiceBlock._
import temple.ast.AttributeType._
import temple.ast.Metadata.{Database, ServiceAuth}
import temple.ast._
import temple.generate.FileSystem.{File, FileContent}
import temple.utils.FileUtils

import scala.collection.immutable.ListMap

object ProjectBuilderTestData {

  private val simpleServiceAttributes = ListMap(
    "intField"      -> Attribute(IntType()),
    "doubleField"   -> Attribute(FloatType()),
    "stringField"   -> Attribute(StringType()),
    "boolField"     -> Attribute(BoolType),
    "dateField"     -> Attribute(DateType),
    "timeField"     -> Attribute(TimeType),
    "dateTimeField" -> Attribute(DateTimeType),
    "blobField"     -> Attribute(BlobType()),
  )

  private val complexServiceAttributes = ListMap(
    "smallIntField"      -> Attribute(IntType(max = Some(100), min = Some(10), precision = 2)),
    "intField"           -> Attribute(IntType(max = Some(100), min = Some(10))),
    "bigIntField"        -> Attribute(IntType(max = Some(100), min = Some(10), precision = 8)),
    "floatField"         -> Attribute(FloatType(max = Some(300), min = Some(0), precision = 4)),
    "doubleField"        -> Attribute(FloatType(max = Some(123), min = Some(0))),
    "stringField"        -> Attribute(StringType(max = None, min = Some(1))),
    "boundedStringField" -> Attribute(StringType(max = Some(5), min = Some(0))),
    "boolField"          -> Attribute(BoolType),
    "dateField"          -> Attribute(DateType),
    "timeField"          -> Attribute(TimeType),
    "dateTimeField"      -> Attribute(DateTimeType),
    "blobField"          -> Attribute(BlobType()),
  )

  val simpleTemplefile: Templefile = Templefile(
    "SampleProject",
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresProject: Templefile = Templefile(
    "SampleProject",
    ProjectBlock(Seq(Database.Postgres)),
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes),
    ),
  )

  val simpleTemplefilePostgresService: Templefile = Templefile(
    "SampleProject",
    services = Map(
      "TempleUser" -> ServiceBlock(simpleServiceAttributes, metadata = Seq(Database.Postgres)),
    ),
  )

  val complexTemplefile: Templefile = Templefile(
    "SampleComplexProject",
    services = Map(
      "ComplexUser" -> ServiceBlock(
        complexServiceAttributes,
        metadata = Seq(ServiceAuth.Email),
        structs = Map("TempleUser" -> StructBlock(simpleServiceAttributes)),
      ),
    ),
  )

  val kongFiles: Map[File, FileContent] = Map(
    File("kube/kong", "kong-deployment.yaml")    -> FileUtils.readResources("kong/kong-deployment.yaml"),
    File("kube/kong", "kong-service.yaml")       -> FileUtils.readResources("kong/kong-service.yaml"),
    File("kube/kong", "kong-db-deployment.yaml") -> FileUtils.readResources("kong/kong-db-deployment.yaml"),
    File("kube/kong", "kong-db-service.yaml")    -> FileUtils.readResources("kong/kong-db-service.yaml"),
    File("kube/kong", "kong-migration-job.yaml") -> FileUtils.readResources("kong/kong-migration-job.yaml"),
  )

  val simpleTemplefileAPISpec: String =
    FileUtils.readResources("project-builder-simple/api/sample-project.openapi.yaml")

  val simpleTemplefilePostgresCreateOutput: String =
    FileUtils.readResources("project-builder-simple/temple-user-db/init.sql")

  val simpleTemplefileUsersDockerfile: String = FileUtils.readResources("project-builder-simple/temple-user/Dockerfile")

  val simpleTemplefileKubeDeployment: String =
    FileUtils.readResources("project-builder-simple/kube/temple-user/deployment.yaml")

  val simpleTemplefileKubeService: String =
    FileUtils.readResources("project-builder-simple/kube/temple-user/service.yaml")

  val simpleTemplefileKubeDbDeployment: String =
    FileUtils.readResources("project-builder-simple/kube/temple-user/db-deployment.yaml")

  val simpleTemplefileKubeDbStorage: String =
    FileUtils.readResources("project-builder-simple/kube/temple-user/db-storage.yaml")

  val simpleTemplefileKubeDbService: String =
    FileUtils.readResources("project-builder-simple/kube/temple-user/db-service.yaml")

  val simpleTemplefileConfigureKong: String = FileUtils.readResources("project-builder-simple/kong/configure-kong.sh")

  val simpleTemplefileGrafanaDashboard: String =
    FileUtils.readResources("project-builder-simple/grafana/provisioning/dashboards/temple-user.json").stripLineEnd

  val simpleTemplefileGrafanaDashboardConfig: String =
    FileUtils.readResources("project-builder-simple/grafana/provisioning/dashboards/dashboards.yml")

  val simpleTemplefileTempleUserGoFile: String =
    FileUtils.readResources("project-builder-simple/temple-user/temple-user.go")
  val simpleTemplefileHookGoFile: String = FileUtils.readResources("project-builder-simple/temple-user/hook.go")
  val simpleTemplefileGoModFile: String  = FileUtils.readResources("project-builder-simple/temple-user/go.mod")
  val simpleTemplefileDaoFile: String    = FileUtils.readResources("project-builder-simple/temple-user/dao/dao.go")

  val simpleTemplefileErrorsFile: String =
    FileUtils.readResources("project-builder-simple/temple-user/dao/errors.go")

  val simpleTemplefileUtilFile: String =
    FileUtils.readResources("project-builder-simple/temple-user/util/util.go")

  val simpleTemplefileMetricFile: String =
    FileUtils.readResources("project-builder-simple/temple-user/metric/metric.go")

  val simpleTemplefileGrafanaDatasourceConfig: String =
    FileUtils.readResources("project-builder-simple/grafana/provisioning/datasources/datasource.yml")

  val simpleTemplefilePrometheusConfig: String =
    FileUtils.readResources("project-builder-simple/prometheus/prometheus.yml")

  val complexTemplefileAPISpec: String =
    FileUtils.readResources("project-builder-complex/api/sample-project.openapi.yaml")

  val complexTemplefilePostgresCreateOutput: String =
    FileUtils.readResources("project-builder-complex/complex-user-db/init.sql")

  val complexTemplefileUsersDockerfile: String =
    FileUtils.readResources("project-builder-complex/complex-user/Dockerfile")

  val complexTemplefileKubeDeployment: String =
    FileUtils.readResources("project-builder-complex/kube/complex-user/deployment.yaml")

  val complexTemplefileKubeDbDeployment: String =
    FileUtils.readResources("project-builder-complex/kube/complex-user/db-deployment.yaml")

  val complexTemplefileKubeService: String =
    FileUtils.readResources("project-builder-complex/kube/complex-user/service.yaml")

  val complexTemplefileKubeDbStorage: String =
    FileUtils.readResources("project-builder-complex/kube/complex-user/db-storage.yaml")

  val complexTemplefileKubeDbService: String =
    FileUtils.readResources("project-builder-complex/kube/complex-user/db-service.yaml")

  val complexTemplefileConfigureKong: String =
    FileUtils.readResources("project-builder-complex/kong/configure-kong.sh")

  val complexTemplefileGrafanaDashboard: String =
    FileUtils.readResources("project-builder-complex/grafana/provisioning/dashboards/complex-user.json").init

  val complexTemplefileAuthGrafanaDashboard: String =
    FileUtils.readResources("project-builder-complex/grafana/provisioning/dashboards/auth.json").init

  val complexTemplefileGrafanaDashboardConfig: String =
    FileUtils.readResources("project-builder-complex/grafana/provisioning/dashboards/dashboards.yml")

  val complexTemplefileTempleUserGoFile: String =
    FileUtils.readResources("project-builder-complex/complex-user/complex-user.go.snippet")

  val complexTemplefileHookGoFile: String =
    FileUtils.readResources("project-builder-complex/complex-user/hook.go.snippet")

  val complexTemplefileGoModFile: String =
    FileUtils.readResources("project-builder-complex/complex-user/go.mod.snippet")

  val complexTemplefileDaoFile: String =
    FileUtils.readResources("project-builder-complex/complex-user/dao/dao.go.snippet")

  val complexTemplefileErrorsFile: String =
    FileUtils.readResources("project-builder-complex/complex-user/dao/errors.go.snippet")

  val complexTemplefileUtilFile: String =
    FileUtils.readResources("project-builder-complex/complex-user/util/util.go.snippet")

  val complexTemplefileMetricFile: String =
    FileUtils.readResources("project-builder-complex/complex-user/metric/metric.go.snippet")

  val complexTemplefileAuthGoFile: String     = FileUtils.readResources("project-builder-complex/auth/auth.go.snippet")
  val complexTemplefileAuthHookGoFile: String = FileUtils.readResources("project-builder-complex/auth/hook.go.snippet")
  val complexTemplefileAuthGoModFile: String  = FileUtils.readResources("project-builder-complex/auth/go.mod.snippet")

  val complexTemplefileAuthUtilFile: String =
    FileUtils.readResources("project-builder-complex/auth/util/util.go.snippet")
  val complexTemplefileAuthDaoFile: String = FileUtils.readResources("project-builder-complex/auth/dao/dao.go.snippet")

  val complexTemplefileAuthErrorsFile: String =
    FileUtils.readResources("project-builder-complex/auth/dao/errors.go.snippet")

  val complexTemplefileAuthHandlerFile: String =
    FileUtils.readResources("project-builder-complex/auth/comm/handler.go.snippet")

  val complexTemplefileAuthMetricFile: String =
    FileUtils.readResources("project-builder-complex/auth/metric/metric.go.snippet")

  val complexTemplefileGrafanaDatasourceConfig: String =
    FileUtils.readResources("project-builder-complex/grafana/provisioning/datasources/datasource.yml")

  val complexTemplefilePrometheusConfig: String =
    FileUtils.readResources("project-builder-complex/prometheus/prometheus.yml")

  val complexTemplefilePostgresAuthOutput: String =
    FileUtils.readResources("project-builder-complex/auth-db/init.sql")

  val complexTemplefileAuthDockerfile: String =
    FileUtils.readResources("project-builder-complex/auth/Dockerfile")

  val complexTemplefileAuthKubeDeployment: String =
    FileUtils.readResources("project-builder-complex/kube/auth/deployment.yaml")

  val complexTemplefileAuthKubeService: String =
    FileUtils.readResources("project-builder-complex/kube/auth/service.yaml")

  val complexTemplefileAuthKubeDbDeployment: String =
    FileUtils.readResources("project-builder-complex/kube/auth/db-deployment.yaml")

  val complexTemplefileAuthKubeDbService: String =
    FileUtils.readResources("project-builder-complex/kube/auth/db-service.yaml")

  val complexTemplefileAuthKubeDbStorage: String =
    FileUtils.readResources("project-builder-complex/kube/auth/db-storage.yaml")
}
