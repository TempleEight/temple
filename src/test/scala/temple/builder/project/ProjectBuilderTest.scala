package temple.builder.project

import org.scalatest.FlatSpec
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.FileMatchers
import temple.generate.FileSystem.File

class ProjectBuilderTest extends FlatSpec with FileMatchers {

  behavior of "ProjectBuilder"

  it should "correctly create a simple project using postgres as the default" in {
    filesShouldMatch(
      ProjectBuilder.build(ProjectBuilderTestData.simpleTemplefile, GoLanguageDetail("github.com/squat/and/dab")).files,
      Map(
        File("temple-user-db", "init.sql")                          -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("temple-user", "Dockerfile")                           -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("api", "sample-project.openapi.yaml")                  -> ProjectBuilderTestData.simpleTemplefileAPISpec,
        File("temple-user", "temple-user.go")                       -> ProjectBuilderTestData.simpleTemplefileTempleUserGoFile,
        File("temple-user", "hook.go")                              -> ProjectBuilderTestData.simpleTemplefileHookGoFile,
        File("temple-user", "go.mod")                               -> ProjectBuilderTestData.simpleTemplefileGoModFile,
        File("temple-user/dao", "dao.go")                           -> ProjectBuilderTestData.simpleTemplefileDaoFile,
        File("temple-user/dao", "errors.go")                        -> ProjectBuilderTestData.simpleTemplefileErrorsFile,
        File("temple-user/util", "util.go")                         -> ProjectBuilderTestData.simpleTemplefileUtilFile,
        File("temple-user/metric", "metric.go")                     -> ProjectBuilderTestData.simpleTemplefileMetricFile,
        File("kube/temple-user", "deployment.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/temple-user", "db-deployment.yaml")              -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/temple-user", "service.yaml")                    -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/temple-user", "db-service.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/temple-user", "db-storage.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")                           -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
        File("grafana/provisioning/dashboards", "temple-user.json") -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboard,
        File("grafana/provisioning/dashboards", "dashboards.yml")   -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboardConfig,
        File("grafana/provisioning/datasources", "datasource.yml")  -> ProjectBuilderTestData.simpleTemplefileGrafanaDatasourceConfig,
        File("prometheus", "prometheus.yml")                        -> ProjectBuilderTestData.simpleTemplefilePrometheusConfig,
      ) ++ ProjectBuilderTestData.kongFiles,
    )
  }

  it should "use postgres when defined at the project level" in {
    projectFilesShouldMatch(
      ProjectBuilder.build(
        ProjectBuilderTestData.simpleTemplefilePostgresProject,
        GoLanguageDetail("github.com/squat/and/dab"),
      ),
      Map(
        File("temple-user-db", "init.sql")                          -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("temple-user", "Dockerfile")                           -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("api", "sample-project.openapi.yaml")                  -> ProjectBuilderTestData.simpleTemplefileAPISpec,
        File("temple-user", "temple-user.go")                       -> ProjectBuilderTestData.simpleTemplefileTempleUserGoFile,
        File("temple-user", "hook.go")                              -> ProjectBuilderTestData.simpleTemplefileHookGoFile,
        File("temple-user", "go.mod")                               -> ProjectBuilderTestData.simpleTemplefileGoModFile,
        File("temple-user/dao", "dao.go")                           -> ProjectBuilderTestData.simpleTemplefileDaoFile,
        File("temple-user/dao", "errors.go")                        -> ProjectBuilderTestData.simpleTemplefileErrorsFile,
        File("temple-user/util", "util.go")                         -> ProjectBuilderTestData.simpleTemplefileUtilFile,
        File("temple-user/metric", "metric.go")                     -> ProjectBuilderTestData.simpleTemplefileMetricFile,
        File("kube/temple-user", "deployment.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/temple-user", "db-deployment.yaml")              -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/temple-user", "service.yaml")                    -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/temple-user", "db-service.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/temple-user", "db-storage.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")                           -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
        File("grafana/provisioning/dashboards", "temple-user.json") -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboard,
        File("grafana/provisioning/dashboards", "dashboards.yml")   -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboardConfig,
        File("grafana/provisioning/datasources", "datasource.yml")  -> ProjectBuilderTestData.simpleTemplefileGrafanaDatasourceConfig,
        File("prometheus", "prometheus.yml")                        -> ProjectBuilderTestData.simpleTemplefilePrometheusConfig,
      ) ++ ProjectBuilderTestData.kongFiles,
    )
  }

  it should "use postgres when defined at the service level" in {
    projectFilesShouldMatch(
      ProjectBuilder
        .build(ProjectBuilderTestData.simpleTemplefilePostgresService, GoLanguageDetail("github.com/squat/and/dab")),
      Map(
        File("temple-user-db", "init.sql")                          -> ProjectBuilderTestData.simpleTemplefilePostgresCreateOutput,
        File("temple-user", "Dockerfile")                           -> ProjectBuilderTestData.simpleTemplefileUsersDockerfile,
        File("api", "sample-project.openapi.yaml")                  -> ProjectBuilderTestData.simpleTemplefileAPISpec,
        File("temple-user", "temple-user.go")                       -> ProjectBuilderTestData.simpleTemplefileTempleUserGoFile,
        File("temple-user", "hook.go")                              -> ProjectBuilderTestData.simpleTemplefileHookGoFile,
        File("temple-user", "go.mod")                               -> ProjectBuilderTestData.simpleTemplefileGoModFile,
        File("temple-user/dao", "dao.go")                           -> ProjectBuilderTestData.simpleTemplefileDaoFile,
        File("temple-user/dao", "errors.go")                        -> ProjectBuilderTestData.simpleTemplefileErrorsFile,
        File("temple-user/util", "util.go")                         -> ProjectBuilderTestData.simpleTemplefileUtilFile,
        File("temple-user/metric", "metric.go")                     -> ProjectBuilderTestData.simpleTemplefileMetricFile,
        File("kube/temple-user", "deployment.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDeployment,
        File("kube/temple-user", "db-deployment.yaml")              -> ProjectBuilderTestData.simpleTemplefileKubeDbDeployment,
        File("kube/temple-user", "service.yaml")                    -> ProjectBuilderTestData.simpleTemplefileKubeService,
        File("kube/temple-user", "db-service.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDbService,
        File("kube/temple-user", "db-storage.yaml")                 -> ProjectBuilderTestData.simpleTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")                           -> ProjectBuilderTestData.simpleTemplefileConfigureKong,
        File("grafana/provisioning/dashboards", "temple-user.json") -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboard,
        File("grafana/provisioning/dashboards", "dashboards.yml")   -> ProjectBuilderTestData.simpleTemplefileGrafanaDashboardConfig,
        File("grafana/provisioning/datasources", "datasource.yml")  -> ProjectBuilderTestData.simpleTemplefileGrafanaDatasourceConfig,
        File("prometheus", "prometheus.yml")                        -> ProjectBuilderTestData.simpleTemplefilePrometheusConfig,
      ) ++ ProjectBuilderTestData.kongFiles,
    )
  }

  it should "correctly create a complex service with nested struct" in {
    projectFilesShouldMatch(
      ProjectBuilder.build(ProjectBuilderTestData.complexTemplefile, GoLanguageDetail("github.com/squat/and/dab")),
      Map(
        File("complex-user-db", "init.sql")                          -> ProjectBuilderTestData.complexTemplefilePostgresCreateOutput,
        File("complex-user", "Dockerfile")                           -> ProjectBuilderTestData.complexTemplefileUsersDockerfile,
        File("api", "sample-complex-project.openapi.yaml")           -> ProjectBuilderTestData.complexTemplefileAPISpec,
        File("complex-user", "complex-user.go")                      -> ProjectBuilderTestData.complexTemplefileTempleUserGoFile,
        File("complex-user", "hook.go")                              -> ProjectBuilderTestData.complexTemplefileHookGoFile,
        File("complex-user", "go.mod")                               -> ProjectBuilderTestData.complexTemplefileGoModFile,
        File("complex-user/dao", "dao.go")                           -> ProjectBuilderTestData.complexTemplefileDaoFile,
        File("complex-user/dao", "errors.go")                        -> ProjectBuilderTestData.complexTemplefileErrorsFile,
        File("complex-user/util", "util.go")                         -> ProjectBuilderTestData.complexTemplefileUtilFile,
        File("complex-user/metric", "metric.go")                     -> ProjectBuilderTestData.complexTemplefileMetricFile,
        File("auth", "auth.go")                                      -> ProjectBuilderTestData.complexTemplefileAuthGoFile,
        File("auth", "go.mod")                                       -> ProjectBuilderTestData.complexTemplefileAuthGoModFile,
        File("auth/util", "util.go")                                 -> ProjectBuilderTestData.complexTemplefileAuthUtilFile,
        File("auth/dao", "dao.go")                                   -> ProjectBuilderTestData.complexTemplefileAuthDaoFile,
        File("auth/dao", "errors.go")                                -> ProjectBuilderTestData.complexTemplefileAuthErrorsFile,
        File("auth/comm", "handler.go")                              -> ProjectBuilderTestData.complexTemplefileAuthHandlerFile,
        File("kube/complex-user", "deployment.yaml")                 -> ProjectBuilderTestData.complexTemplefileKubeDeployment,
        File("kube/complex-user", "db-deployment.yaml")              -> ProjectBuilderTestData.complexTemplefileKubeDbDeployment,
        File("kube/complex-user", "service.yaml")                    -> ProjectBuilderTestData.complexTemplefileKubeService,
        File("kube/complex-user", "db-service.yaml")                 -> ProjectBuilderTestData.complexTemplefileKubeDbService,
        File("kube/complex-user", "db-storage.yaml")                 -> ProjectBuilderTestData.complexTemplefileKubeDbStorage,
        File("kong", "configure-kong.sh")                            -> ProjectBuilderTestData.complexTemplefileConfigureKong,
        File("grafana/provisioning/dashboards", "complex-user.json") -> ProjectBuilderTestData.complexTemplefileGrafanaDashboard,
        File("grafana/provisioning/dashboards", "dashboards.yml")    -> ProjectBuilderTestData.complexTemplefileGrafanaDashboardConfig,
        File("grafana/provisioning/datasources", "datasource.yml")   -> ProjectBuilderTestData.complexTemplefileGrafanaDatasourceConfig,
        File("prometheus", "prometheus.yml")                         -> ProjectBuilderTestData.complexTemplefilePrometheusConfig,
      ) ++ ProjectBuilderTestData.kongFiles,
    )
  }
}
