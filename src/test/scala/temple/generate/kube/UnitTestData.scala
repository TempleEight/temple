package temple.generate.kube

import temple.generate.kube.ast.OrchestrationType._
import temple.utils.FileUtils

object UnitTestData {

  val basicOrchestrationRoot: OrchestrationRoot = OrchestrationRoot(
    Seq(
      Service(
        name = "user",
        image = "temple-user-service",
        dbImage = "postgres:12.1",
        ports = Seq("api" -> 80),
        replicas = 1,
        secretName = "regcred",
        envVars = Seq("PGUSER" -> "postgres"),
      ),
    ),
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
      |  name: user-db
      |  labels:
      |    app: user
      |    type: local""".stripMargin

  val userDbStorageClaimHeader: String =
    """apiVersion: v1
      |kind: PersistentVolumeClaim
      |metadata:
      |  name: user-db
      |  labels:
      |    app: user""".stripMargin

  val userDeployment: String = FileUtils.readResources("kube/user-deployment.yaml")

  val userService: String = FileUtils.readResources("kube/user-service.yaml")

  val userDbDeployment: String = FileUtils.readResources("kube/user-db-deployment.yaml")
}
