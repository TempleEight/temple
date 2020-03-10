package temple.generate.kube

import temple.generate.kube.ast.{OrchestrationRoot, Service}

object UnitTestData {

  val basicOrchestrationRoot: OrchestrationRoot = OrchestrationRoot(
    Seq(
      Service("user", "temple-user-service", Seq(80)),
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
      |    app: user-db""".stripMargin

  val userDbServiceHeader: String =
    """apiVersion: v1
      |kind: Service
      |metadata:
      |  name: user-db
      |  labels:
      |    app: user-db""".stripMargin

  val userDbStorageVolumeHeader: String =
    """apiVersion: v1
      |kind: PersistentVolume
      |metadata:
      |  name: user-db
      |  labels:
      |    app: user-db
      |    type: local""".stripMargin

  val userDbStorageClaimHeader: String =
    """apiVersion: v1
      |kind: PersistentVolumeClaim
      |metadata:
      |  name: user-db
      |  labels:
      |    app: user-db""".stripMargin
}
