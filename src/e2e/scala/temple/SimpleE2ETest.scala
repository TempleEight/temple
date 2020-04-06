package temple

import java.nio.file.{Files, Paths}

import org.scalatest.{FlatSpec, Matchers}
import temple.detail.PoliceSergeantNicholasAngel
import temple.utils.FileUtils

import scala.jdk.StreamConverters._
import scala.reflect.io.Directory

class SimpleE2ETest extends FlatSpec with Matchers {

  behavior of "Temple"

  it should "generate Postgres, Docker and Kube scripts" in {
    // Clean up folder if exists
    val basePath  = Paths.get("/tmp/temple-e2e-test-1")
    val directory = new Directory(basePath.toFile)
    directory.deleteRecursively()

    noException should be thrownBy Application.generate(
      new TempleConfig(
        Seq("generate", "-o", basePath.toAbsolutePath.toString, "src/test/scala/temple/testfiles/simple.temple"),
      ),
      PoliceSergeantNicholasAngel,
    )

    // Exactly these folders should have been generated
    val expectedFolders =
      Set(
        "simple-temple-test-user-db",
        "simple-temple-test-user",
        "auth",
        "auth-db",
        "api",
        "booking-db",
        "booking",
        "simple-temple-test-group-db",
        "simple-temple-test-group",
        "kong",
        "kube",
        "grafana",
        "prometheus",
      ).map(dir => basePath.resolve(dir))
    Files.list(basePath).toScala(Set) shouldBe expectedFolders

    // Only one file should be present in the temple-user-db folder
    val expectedTempleUserDbFiles =
      Set("init.sql").map(dir => basePath.resolve("simple-temple-test-user-db").resolve(dir))
    Files.list(basePath.resolve("simple-temple-test-user-db")).toScala(Set) shouldBe expectedTempleUserDbFiles

    // The content of the simple-temple-test-user-db/init.sql file should be correct
    val initSql = Files.readString(basePath.resolve("simple-temple-test-user-db").resolve("init.sql"))
    initSql shouldBe SimpleE2ETestData.createStatement

    // Only these files should be present in the simple-temple-test-user folder
    val expectedTempleUserFiles =
      Set("Dockerfile", "dao", "simple-temple-test-user.go", "util", "go.mod", "metric", "hook.go").map(dir =>
        basePath.resolve("simple-temple-test-user").resolve(dir),
      )
    Files.list(basePath.resolve("simple-temple-test-user")).toScala(Set) shouldBe expectedTempleUserFiles

    // The content of the simple-temple-test-user/Dockerfile file should be correct
    val templeUserDockerfile = Files.readString(basePath.resolve("simple-temple-test-user").resolve("Dockerfile"))
    templeUserDockerfile shouldBe SimpleE2ETestData.dockerfile

    // The content of the main simple-temple-test-user go file should be correct
    val templeUserGoFile =
      Files.readString(basePath.resolve("simple-temple-test-user").resolve("simple-temple-test-user.go"))
    templeUserGoFile shouldBe FileUtils.readResources("go/user/user.go.snippet")

    // The content of hook.go should be correct
    val templeUserHookGoFile = Files.readString(basePath.resolve("simple-temple-test-user").resolve("hook.go"))
    templeUserHookGoFile shouldBe FileUtils.readResources("go/user/hook.go.snippet")

    // The content of the go.mod should be correct
    val templeUserGoModFile = Files.readString(basePath.resolve("simple-temple-test-user").resolve("go.mod"))
    templeUserGoModFile shouldBe FileUtils.readResources("go/user/go.mod.snippet")

    // Only these files should be present in the simple-temple-test-user/dao folder
    val expectedTempleUserDaoFiles =
      Set("dao.go", "errors.go").map(dir => basePath.resolve("simple-temple-test-user/dao").resolve(dir))
    Files.list(basePath.resolve("simple-temple-test-user/dao")).toScala(Set) shouldBe expectedTempleUserDaoFiles

    // The content of the simple-temple-test-user dao file should be correct
    val templeUserDaoFile = Files.readString(basePath.resolve("simple-temple-test-user/dao").resolve("dao.go"))
    templeUserDaoFile shouldBe FileUtils.readResources("go/user/dao/dao.go.snippet")

    // The content of the simple-temple-test-user dao errors file should be correct
    val templeUserErrorsFile = Files.readString(basePath.resolve("simple-temple-test-user/dao").resolve("errors.go"))
    templeUserErrorsFile shouldBe FileUtils.readResources("go/user/dao/errors.go.snippet")

    // Only these files should be present in the simple-temple-test-user/util folder
    val expectedTempleUserUtilFiles =
      Set("util.go").map(dir => basePath.resolve("simple-temple-test-user/util").resolve(dir))
    Files.list(basePath.resolve("simple-temple-test-user/util")).toScala(Set) shouldBe expectedTempleUserUtilFiles

    // The content of the simple-temple-test-user util file should be correct
    val templeUserUtilFile = Files.readString(basePath.resolve("simple-temple-test-user/util").resolve("util.go"))
    templeUserUtilFile shouldBe FileUtils.readResources("go/user/util/util.go.snippet")

    // Only these files should be present in the simple-temple-test-user/metric folder
    val expectedTempleUserMetricFiles =
      Set("metric.go").map(dir => basePath.resolve("simple-temple-test-user/metric").resolve(dir))
    Files.list(basePath.resolve("simple-temple-test-user/metric")).toScala(Set) shouldBe expectedTempleUserMetricFiles

    // The content of the simple-temple-test-user util file should be correct
    val templeUserMetricFile = Files.readString(basePath.resolve("simple-temple-test-user/metric").resolve("metric.go"))

    templeUserMetricFile shouldBe FileUtils.readResources("go/user/metric/metric.go.snippet")

    // Only one file should be present in the kong folder
    val expectedKongFiles = Set("configure-kong.sh").map(dir => basePath.resolve("kong").resolve(dir))
    Files.list(basePath.resolve("kong")).toScala(Set) shouldBe expectedKongFiles

    // The content of the kong/configure-kong.sh file should be correct
    val templeKongConf = Files.readString(basePath.resolve("kong").resolve("configure-kong.sh"))
    templeKongConf shouldBe SimpleE2ETestData.configureKong

    // Only these files should be present in the kube/kong folder
    val expectedKongKubeFiles = Set(
      "kong-deployment.yaml",
      "kong-service.yaml",
      "kong-db-deployment.yaml",
      "kong-db-service.yaml",
      "kong-migration-job.yaml",
    ).map(dir => basePath.resolve("kube/kong").resolve(dir))
    Files.list(basePath.resolve("kube/kong")).toScala(Set) shouldBe expectedKongKubeFiles

    // The content of the kube/kong/ files should be correct
    val templeKongKubeDeployment = Files.readString(basePath.resolve("kube/kong").resolve("kong-deployment.yaml"))
    templeKongKubeDeployment shouldBe FileUtils.readResources("kube/kong/kong-deployment.yaml")

    val templeKongKubeDbDeployment = Files.readString(basePath.resolve("kube/kong").resolve("kong-db-deployment.yaml"))
    templeKongKubeDbDeployment shouldBe FileUtils.readResources("kube/kong/kong-db-deployment.yaml")

    val templeKongKubeService = Files.readString(basePath.resolve("kube/kong").resolve("kong-service.yaml"))
    templeKongKubeService shouldBe FileUtils.readResources("kube/kong/kong-service.yaml")

    val templeKongKubeDbService = Files.readString(basePath.resolve("kube/kong").resolve("kong-db-service.yaml"))
    templeKongKubeDbService shouldBe FileUtils.readResources("kube/kong/kong-db-service.yaml")

    val templeKongKubeMigration = Files.readString(basePath.resolve("kube/kong").resolve("kong-migration-job.yaml"))
    templeKongKubeMigration shouldBe FileUtils.readResources("kube/kong/kong-migration-job.yaml")

    // Only these files should be present in the kube/temple-user folder
    val expectedUserKubeFiles = Set(
      "deployment.yaml",
      "service.yaml",
      "db-deployment.yaml",
      "db-service.yaml",
      "db-storage.yaml",
    ).map(dir => basePath.resolve("kube/simple-temple-test-user").resolve(dir))
    Files.list(basePath.resolve("kube/simple-temple-test-user")).toScala(Set) shouldBe expectedUserKubeFiles

    // The content of the kube/temple-user/ files should be correct
    val templeUserKubeDeployment =
      Files.readString(basePath.resolve("kube/simple-temple-test-user").resolve("deployment.yaml"))
    templeUserKubeDeployment shouldBe SimpleE2ETestData.kubeDeployment

    val templeUserKubeDbDeployment =
      Files.readString(basePath.resolve("kube/simple-temple-test-user").resolve("db-deployment.yaml"))
    templeUserKubeDbDeployment shouldBe SimpleE2ETestData.kubeDbDeployment

    val templeUserKubeService =
      Files.readString(basePath.resolve("kube/simple-temple-test-user").resolve("service.yaml"))
    templeUserKubeService shouldBe SimpleE2ETestData.kubeService

    val templeUserKubeDbService =
      Files.readString(basePath.resolve("kube/simple-temple-test-user").resolve("db-service.yaml"))
    templeUserKubeDbService shouldBe SimpleE2ETestData.kubeDbService

    val templeUserKubeStorage =
      Files.readString(basePath.resolve("kube/simple-temple-test-user").resolve("db-storage.yaml"))
    templeUserKubeStorage shouldBe SimpleE2ETestData.kubeDbStorage

    // Only these folders should be present in the grafana folder
    val expectedGrafanaFolders = Set("provisioning").map(dir => basePath.resolve("grafana").resolve(dir))
    Files.list(basePath.resolve("grafana")).toScala(Set) shouldBe expectedGrafanaFolders

    // Only these folders should be present in the grafana/provisioning folder
    val expectedGrafanaProvisioningFolders =
      Set("dashboards", "datasources").map(dir => basePath.resolve("grafana/provisioning").resolve(dir))
    Files.list(basePath.resolve("grafana/provisioning")).toScala(Set) shouldBe expectedGrafanaProvisioningFolders

    // Only these files should be present in the grafana/provisioning/dashboards folder
    val expectedGrafanaDashboardsFolders =
      Set(
        "booking.json",
        "simple-temple-test-group.json",
        "simple-temple-test-user.json",
        "auth.json",
        "dashboards.yml",
      ).map(dir => basePath.resolve("grafana/provisioning/dashboards").resolve(dir))

    Files
      .list(basePath.resolve("grafana/provisioning/dashboards"))
      .toScala(Set) shouldBe expectedGrafanaDashboardsFolders

    // The content of the grafana/provisioning/dashboards files should be correct
    val templeUserGrafanaDashboard =
      Files.readString(basePath.resolve("grafana/provisioning/dashboards").resolve("simple-temple-test-user.json"))
    templeUserGrafanaDashboard shouldBe SimpleE2ETestData.grafanaDashboard

    val templeUserGrafanaDashboardConfig =
      Files.readString(basePath.resolve("grafana/provisioning/dashboards").resolve("dashboards.yml"))
    templeUserGrafanaDashboardConfig shouldBe SimpleE2ETestData.grafanaDashboardConfig

    // Only these files should be present in the grafana/provisioning/datasources folder
    val expectedGrafanaDatasourcesFolders =
      Set("datasource.yml").map(dir => basePath.resolve("grafana/provisioning/datasources").resolve(dir))

    Files
      .list(basePath.resolve("grafana/provisioning/datasources"))
      .toScala(Set) shouldBe expectedGrafanaDatasourcesFolders

    // The content of the grafana/provisioning/datasources files should be correct
    val templeUserGrafanaDatasourceConfig =
      Files.readString(basePath.resolve("grafana/provisioning/datasources").resolve("datasource.yml"))
    templeUserGrafanaDatasourceConfig shouldBe SimpleE2ETestData.grafanaDatasourceConfig

    // Only these files should be present in the prometheus folder
    val expectedPrometheusFolders = Set("prometheus.yml").map(dir => basePath.resolve("prometheus").resolve(dir))
    Files.list(basePath.resolve("prometheus")).toScala(Set) shouldBe expectedPrometheusFolders

    // The content of the prometheus/prometheus.yml file should be correct
    val templeUserPrometheusConfig =
      Files.readString(basePath.resolve("prometheus").resolve("prometheus.yml"))
    templeUserPrometheusConfig shouldBe SimpleE2ETestData.prometheusConfig

    // Exactly these files should be in the auth directory
    val exepectedAuthFolderFiles =
      Set("Dockerfile", "auth.go", "go.mod", "hook.go", "dao", "comm", "util", "metric").map(dir =>
        basePath.resolve("auth").resolve(dir),
      )

    Files
      .list(basePath.resolve("auth"))
      .toScala(Set) shouldBe exepectedAuthFolderFiles

    // the content of the auth/Dockerfile should be correct
    val authDockerfile =
      Files.readString(basePath.resolve("auth").resolve("Dockerfile"))
    authDockerfile shouldBe SimpleE2ETestData.authDockerfile

    // The content of the auth/auth.go file should be correct
    val authGoFile =
      Files.readString(basePath.resolve("auth").resolve("auth.go"))
    authGoFile shouldBe SimpleE2ETestData.authGoFile

    // The content of the auth/go.mod file should be correct
    val authGoModFile =
      Files.readString(basePath.resolve("auth").resolve("go.mod"))
    authGoModFile shouldBe SimpleE2ETestData.authGoModFile

    // The content of the auth/hook.go file should be correct
    val authHookFile =
      Files.readString(basePath.resolve("auth").resolve("hook.go"))
    authHookFile shouldBe SimpleE2ETestData.authHookFile

    // Only this file should be in the auth/util directory
    val expectedAuthUtilFiles = Set("util.go").map(dir => basePath.resolve("auth/util").resolve(dir))

    Files.list(basePath.resolve("auth/util")).toScala(Set) shouldBe expectedAuthUtilFiles

    // The content of the auth/util/util.go file should be correct
    val authUtilFile = Files.readString(basePath.resolve("auth/util").resolve("util.go"))
    authUtilFile shouldBe SimpleE2ETestData.authUtilFile

    // Only this file should be in the auth/comm directory
    val expectedAuthCommFiles = Set("handler.go").map(dir => basePath.resolve("auth/comm").resolve(dir))

    Files.list(basePath.resolve("auth/comm")).toScala(Set) shouldBe expectedAuthCommFiles

    // The content of the auth/util/util.go file should be correct
    val authHandlerFile = Files.readString(basePath.resolve("auth/comm").resolve("handler.go"))
    authHandlerFile shouldBe SimpleE2ETestData.authHandlerFile

    // Only this file should be in the auth/dao directory
    val expectedAuthDaoFiles = Set("dao.go", "errors.go").map(dir => basePath.resolve("auth/dao").resolve(dir))

    Files.list(basePath.resolve("auth/dao")).toScala(Set) shouldBe expectedAuthDaoFiles

    // The content of the auth/util/util.go file should be correct
    val authDaoFile = Files.readString(basePath.resolve("auth/dao").resolve("dao.go"))
    authDaoFile shouldBe SimpleE2ETestData.authDaoFile

    // The content of the auth/util/errors.go file should be correct
    val authErrorsFile = Files.readString(basePath.resolve("auth/dao").resolve("errors.go"))
    authErrorsFile shouldBe SimpleE2ETestData.authErrorsFile

    // Only this file should be present in the auth/metric directory
    val expectedAuthMetricFiles = Set("metric.go").map(dir => basePath.resolve("auth/metric").resolve(dir))

    Files.list(basePath.resolve("auth/metric")).toScala(Set) shouldBe expectedAuthMetricFiles

    // The contents of the auth/metric/metric.go file should be correct
    val authMetricFile = Files.readString(basePath.resolve("auth/metric").resolve("metric.go"))
    authMetricFile shouldBe SimpleE2ETestData.authMetricFile

    // The auth-db folder should contain exactly one file
    val expectedAuthDbFiles = Set("init.sql").map(dir => basePath.resolve("auth-db").resolve(dir))
    Files.list(basePath.resolve("auth-db")).toScala(Set) shouldBe expectedAuthDbFiles

    //The auth-db/init.sql file should be correct
    val authDbInitSqlFile = Files.readString(basePath.resolve("auth-db").resolve("init.sql"))
    authDbInitSqlFile shouldBe SimpleE2ETestData.authDbInitSqlFile
  }
}
