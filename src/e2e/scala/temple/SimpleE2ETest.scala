package temple

import java.nio.file.{Files, Paths}

import org.scalatest.{FlatSpec, Matchers}
import temple.detail.MockQuestionAsker
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
      MockQuestionAsker,
    )

    // Exactly these folders should have been generated
    val expectedFolders =
      Set(
        "templeuser-db",
        "templeuser",
        "booking-db",
        "booking",
        "event-db",
        "event",
        "kong",
        "kube",
        "grafana",
        "prometheus",
      ).map(dir => basePath.resolve(dir))
    Files.list(basePath).toScala(Set) shouldBe expectedFolders

    // Only one file should be present in the templeuser-db folder
    val expectedTempleUserDbFiles = Set("init.sql").map(dir => basePath.resolve("templeuser-db").resolve(dir))
    Files.list(basePath.resolve("templeuser-db")).toScala(Set) shouldBe expectedTempleUserDbFiles

    // The content of the templeuser-db/init.sql file should be correct
    val initSql = Files.readString(basePath.resolve("templeuser-db").resolve("init.sql"))
    initSql shouldBe SimpleE2ETestData.createStatement

    // Only one file should be present in the templeuser folder
    val expectedTempleUserFiles =
      Set("Dockerfile", "dao", "templeuser.go", "util", "go.mod").map(dir => basePath.resolve("templeuser").resolve(dir),
      )
    Files.list(basePath.resolve("templeuser")).toScala(Set) shouldBe expectedTempleUserFiles

    // The content of the templeuser/Dockerfile file should be correct
    val templeUserDockerfile = Files.readString(basePath.resolve("templeuser").resolve("Dockerfile"))
    templeUserDockerfile shouldBe SimpleE2ETestData.dockerfile

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
    ).map(dir => basePath.resolve("kube/temple-user").resolve(dir))
    Files.list(basePath.resolve("kube/temple-user")).toScala(Set) shouldBe expectedUserKubeFiles

    // The content of the kube/temple-user/ files should be correct
    val templeUserKubeDeployment = Files.readString(basePath.resolve("kube/temple-user").resolve("deployment.yaml"))
    templeUserKubeDeployment shouldBe SimpleE2ETestData.kubeDeployment

    val templeUserKubeDbDeployment =
      Files.readString(basePath.resolve("kube/temple-user").resolve("db-deployment.yaml"))
    templeUserKubeDbDeployment shouldBe SimpleE2ETestData.kubeDbDeployment

    val templeUserKubeService = Files.readString(basePath.resolve("kube/temple-user").resolve("service.yaml"))
    templeUserKubeService shouldBe SimpleE2ETestData.kubeService

    val templeUserKubeDbService = Files.readString(basePath.resolve("kube/temple-user").resolve("db-service.yaml"))
    templeUserKubeDbService shouldBe SimpleE2ETestData.kubeDbService

    val templeUserKubeStorage = Files.readString(basePath.resolve("kube/temple-user").resolve("db-storage.yaml"))
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
      Set("booking.json", "event.json", "templeuser.json", "dashboards.yml").map(dir =>
        basePath.resolve("grafana/provisioning/dashboards").resolve(dir),
      )

    Files
      .list(basePath.resolve("grafana/provisioning/dashboards"))
      .toScala(Set) shouldBe expectedGrafanaDashboardsFolders

    // The content of the grafana/provisioning/dashboards files should be correct
    val templeUserGrafanaDashboard =
      Files.readString(basePath.resolve("grafana/provisioning/dashboards").resolve("templeuser.json"))
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
  }
}
