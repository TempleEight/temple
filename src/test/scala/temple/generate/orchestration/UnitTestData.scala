package temple.generate.orchestration

import temple.ast.Templefile.Ports
import temple.generate.orchestration.ast.OrchestrationType.{DbStorage, OrchestrationRoot, Service}
import temple.generate.orchestration.kube.ast.LifecycleCommand
import temple.utils.FileUtils

object UnitTestData {

  val basicOrchestrationService: Service = Service(
    name = "user",
    image = "localhost:5000/temple-user-service",
    dbImage = "postgres:12.1",
    ports = Ports(80, 81),
    replicas = 1,
    secretName = "regcred",
    appEnvVars = Seq(),
    dbEnvVars = Seq("PGUSER" -> "postgres"),
    dbStorage = DbStorage(
      dataMount = "/var/lib/postgresql/data",
      initMount = "/docker-entrypoint-initdb.d/init.sql",
      initFile = "init.sql",
      hostPath = "/data/user-db",
    ),
    dbLifecycleCommand = LifecycleCommand.echoDone.toString,
    usesAuth = true,
  )

  val basicOrchestrationRootWithMetrics: OrchestrationRoot = OrchestrationRoot(
    Seq(
      basicOrchestrationService,
    ),
    usesMetrics = true,
  )

  val basicOrchestrationRootWithoutMetrics: OrchestrationRoot = OrchestrationRoot(
    Seq(
      basicOrchestrationService,
    ),
    usesMetrics = false,
  )

  val userDeploymentHeader: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: user
      |  labels:
      |    app: user""".stripMargin

  val userServiceHeader: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: user
      |  labels:
      |    app: user""".stripMargin

  val userDbDeploymentHeader: String =
    """apiVersion: apps/v1
      |kind: Deployment
      |metadata:
      |  name: user-db
      |  labels:
      |    app: user
      |    kind: db""".stripMargin

  val userDbServiceHeader: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: user-db
      |  labels:
      |    app: user
      |    kind: db""".stripMargin

  val userDbStorageVolumeHeader: String =
    """apiVersion: v1
      |kind: PersistentVolume
      |metadata:
      |  name: user-db-volume
      |  labels:
      |    app: user
      |    type: local""".stripMargin

  val userDbStorageClaimHeader: String =
    """apiVersion: v1
      |kind: PersistentVolumeClaim
      |metadata:
      |  name: user-db-claim
      |  labels:
      |    app: user""".stripMargin

  val userDeployment: String = FileUtils.readResources("kube/user-deployment.yaml")

  val userService: String = FileUtils.readResources("kube/user-service.yaml")

  val userDbDeployment: String = FileUtils.readResources("kube/user-db-deployment.yaml")

  val userDbService: String = FileUtils.readResources("kube/user-db-service.yaml")

  val userDbStorage: String = FileUtils.readResources("kube/user-db-storage.yaml")

  val userKongConfig: String = FileUtils.readResources("kong/configure-kong.sh")

  val userDeployScriptWithMetrics: String = FileUtils.readResources("shell/deploy.sh")

  val userDeployScriptWithoutMetrics: String = FileUtils.readResources("shell/deploy-without-metrics.sh")

  val userGrafanaDeployment: String = FileUtils.readResources("kube/user-grafana-deployment.yaml")

  val userGrafanaService: String = FileUtils.readResources("project-builder-complex/kube/grafana/grafana-service.yaml")

  val userPromDeployment: String =
    FileUtils.readResources("project-builder-complex/kube/prom/prometheus-deployment.yaml")

  val userPromService: String = FileUtils.readResources("project-builder-complex/kube/prom/prometheus-service.yaml")
}
