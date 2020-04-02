package temple.builder.project

import org.scalatest.{FlatSpec, Matchers}
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.FileSystem._

class ProjectBuilderTest extends FlatSpec with Matchers {

  behavior of "ProjectBuilder"

  it should "correctly create a simple project using postgres as the default" in {
    val project =
      ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefile, GoLanguageDetail("github.com/squat/and/dab"))
    val expected = Map(
        File("templeuser-db", "init.sql")                          -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("templeuser", "Dockerfile")                           -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("api", "SampleProject.openapi.yaml")                  -> ProjectBuilderTestData.simpleTemplefileAPISpec,
        File("templeuser", "templeuser.go")                        -> ProjectBuilderTestData.simpleTemplefileTempleUserGoFile,
        File("templeuser", "go.mod")                               -> ProjectBuilderTestData.simpleTemplefileGoModFile,
        File("templeuser/dao", "dao.go")                           -> ProjectBuilderTestData.simpleTemplefileDaoFile,
        File("templeuser/dao", "errors.go")                        -> ProjectBuilderTestData.simpleTemplefileErrorsFile,
        File("templeuser/util", "util.go")                         -> ProjectBuilderTestData.simpleTemplefileUtilFile,
        File("templeuser/metric", "metric.go")                     -> ProjectBuilderTestData.simpleTemplefileMetricFile,
        File("kube/temple-user", "deployment.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/temple-user", "db-deployment.yaml")             -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/temple-user", "service.yaml")                   -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/temple-user", "db-service.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/temple-user", "db-storage.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")                          -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
        File("grafana/provisioning/dashboards", "templeuser.json") -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboard,
        File("grafana/provisioning/dashboards", "dashboards.yml")  -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboardConfig,
        File("grafana/provisioning/datasources", "datasource.yml") -> ProjectBuilderTestData.simpleTemplefileGrafanaDatasourceConfig,
        File("prometheus", "prometheus.yml")                       -> ProjectBuilderTestData.simpleTemplefilePrometheusConfig,
      ) ++ ProjectBuilderTestData.kongFiles
    project.files shouldBe expected
  }

  it should "use postgres when defined at the project level" in {
    val project = ProjectBuilder
      .build(ProjectBuilderTestData.simpleTemplefilePostgresProject, GoLanguageDetail("github.com/squat/and/dab"))
    val expected = Map(
        File("templeuser-db", "init.sql")                          -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("templeuser", "Dockerfile")                           -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("api", "SampleProject.openapi.yaml")                  -> ProjectBuilderTestData.simpleTemplefileAPISpec,
        File("templeuser", "templeuser.go")                        -> ProjectBuilderTestData.simpleTemplefileTempleUserGoFile,
        File("templeuser", "go.mod")                               -> ProjectBuilderTestData.simpleTemplefileGoModFile,
        File("templeuser/dao", "dao.go")                           -> ProjectBuilderTestData.simpleTemplefileDaoFile,
        File("templeuser/dao", "errors.go")                        -> ProjectBuilderTestData.simpleTemplefileErrorsFile,
        File("templeuser/util", "util.go")                         -> ProjectBuilderTestData.simpleTemplefileUtilFile,
        File("templeuser/metric", "metric.go")                     -> ProjectBuilderTestData.simpleTemplefileMetricFile,
        File("kube/temple-user", "deployment.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/temple-user", "db-deployment.yaml")             -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/temple-user", "service.yaml")                   -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/temple-user", "db-service.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/temple-user", "db-storage.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")                          -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
        File("grafana/provisioning/dashboards", "templeuser.json") -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboard,
        File("grafana/provisioning/dashboards", "dashboards.yml")  -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboardConfig,
        File("grafana/provisioning/datasources", "datasource.yml") -> ProjectBuilderTestData.simpleTemplefileGrafanaDatasourceConfig,
        File("prometheus", "prometheus.yml")                       -> ProjectBuilderTestData.simpleTemplefilePrometheusConfig,
      ) ++ ProjectBuilderTestData.kongFiles
    project.files shouldBe expected
  }

  it should "use postgres when defined at the service level" in {
    val project = ProjectBuilder
      .build(ProjectBuilderTestData.simpleTemplefilePostgresService, GoLanguageDetail("github.com/squat/and/dab"))
    val expected = Map(
        File("templeuser-db", "init.sql")                          -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("templeuser", "Dockerfile")                           -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("api", "SampleProject.openapi.yaml")                  -> ProjectBuilderTestData.simpleTemplefileAPISpec,
        File("templeuser", "templeuser.go")                        -> ProjectBuilderTestData.simpleTemplefileTempleUserGoFile,
        File("templeuser", "go.mod")                               -> ProjectBuilderTestData.simpleTemplefileGoModFile,
        File("templeuser/dao", "dao.go")                           -> ProjectBuilderTestData.simpleTemplefileDaoFile,
        File("templeuser/dao", "errors.go")                        -> ProjectBuilderTestData.simpleTemplefileErrorsFile,
        File("templeuser/util", "util.go")                         -> ProjectBuilderTestData.simpleTemplefileUtilFile,
        File("templeuser/metric", "metric.go")                     -> ProjectBuilderTestData.simpleTemplefileMetricFile,
        File("kube/temple-user", "deployment.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/temple-user", "db-deployment.yaml")             -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/temple-user", "service.yaml")                   -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/temple-user", "db-service.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/temple-user", "db-storage.yaml")                -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")                          -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
        File("grafana/provisioning/dashboards", "templeuser.json") -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboard,
        File("grafana/provisioning/dashboards", "dashboards.yml")  -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboardConfig,
        File("grafana/provisioning/datasources", "datasource.yml") -> ProjectBuilderTestData.simpleTemplefileGrafanaDatasourceConfig,
        File("prometheus", "prometheus.yml")                       -> ProjectBuilderTestData.simpleTemplefilePrometheusConfig,
      ) ++ ProjectBuilderTestData.kongFiles
    project.files shouldBe expected
  }

  it should "correctly create a complex service with nested struct" in {
    val project =
      ProjectBuilder.build(ProjectBuilderTestData.complexTemplefile, GoLanguageDetail("github.com/squat/and/dab"))
    project.files shouldBe Map(
      File("complexuser-db", "init.sql")                          -> ProjectBuilderTestData.complexTemplefilePostgresCreateOutput,
      File("complexuser", "Dockerfile")                           -> ProjectBuilderTestData.complexTemplefileUsersDockerfile,
      File("api", "SampleComplexProject.openapi.yaml")            -> ProjectBuilderTestData.complexTemplefileAPISpec,
      File("complexuser", "complexuser.go")                       -> ProjectBuilderTestData.complexTemplefileTempleUserGoFile,
      File("complexuser", "go.mod")                               -> ProjectBuilderTestData.complexTemplefileGoModFile,
      File("complexuser/dao", "dao.go")                           -> ProjectBuilderTestData.complexTemplefileDaoFile,
      File("complexuser/dao", "errors.go")                        -> ProjectBuilderTestData.complexTemplefileErrorsFile,
      File("complexuser/util", "util.go")                         -> ProjectBuilderTestData.complexTemplefileUtilFile,
      File("complexuser/metric", "metric.go")                     -> ProjectBuilderTestData.complexTemplefileMetricFile,
      File("auth", "auth.go")                                     -> ProjectBuilderTestData.complexTemplefileAuthGoFile,
      File("auth", "go.mod")                                      -> ProjectBuilderTestData.complexTemplefileAuthGoModFile,
      File("auth/util", "util.go")                                -> ProjectBuilderTestData.complexTemplefileAuthUtilFile,
      File("auth/dao", "dao.go")                                  -> ProjectBuilderTestData.complexTemplefileAuthDaoFile,
      File("auth/dao", "errors.go")                               -> ProjectBuilderTestData.complexTemplefileAuthErrorsFile,
      File("auth/comm", "handler.go")                             -> ProjectBuilderTestData.complexTemplefileAuthHandlerFile,
      File("kube/complex-user", "deployment.yaml")                -> ProjectBuilderTestData.complexTemplefileKubeDeployment,
      File("kube/complex-user", "db-deployment.yaml")             -> ProjectBuilderTestData.complexTemplefileKubeDbDeployment,
      File("kube/complex-user", "service.yaml")                   -> ProjectBuilderTestData.complexTemplefileKubeService,
      File("kube/complex-user", "db-service.yaml")                -> ProjectBuilderTestData.complexTemplefileKubeDbService,
      File("kube/complex-user", "db-storage.yaml")                -> ProjectBuilderTestData.complexTemplefileKubeDbStorage,
      File("kong", "configure-kong.sh")                           -> ProjectBuilderTestData.complexTemplefileConfigureKong,
      File("grafana/provisioning/dashboards", "complexuser.json") -> ProjectBuilderTestData.complexTemplefileGrafanaDashboard,
      File("grafana/provisioning/dashboards", "dashboards.yml")   -> ProjectBuilderTestData.complexTemplefileGrafanaDashboardConfig,
      File("grafana/provisioning/datasources", "datasource.yml")  -> ProjectBuilderTestData.complexTemplefileGrafanaDatasourceConfig,
      File("prometheus", "prometheus.yml")                        -> ProjectBuilderTestData.complexTemplefilePrometheusConfig,
    ) ++ ProjectBuilderTestData.kongFiles
  }
}
