package temple.generate.kube

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.Printer
import temple.generate.FileSystem._
import temple.generate.kube.ast.OrchestrationType._
import temple.generate.kube.ast.gen.KubeType._
import temple.generate.kube.ast.gen.Spec._
import temple.generate.kube.ast.gen.volume.{AccessMode, ReclaimPolicy, StorageClass}
import temple.generate.kube.ast.gen.{PlacementStrategy, RestartPolicy}
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.FileUtils

/** Generates the Kubernetes config files for each microservice */
object KubernetesGenerator {

  /** Used for outputting correct yaml in the right format */
  private val printer = Printer(preserveOrder = true, dropNullKeys = true)

  /** Generate the header of a Kubernetes yaml config */
  private def generateHeader(service: Service, genType: GenType, isDb: Boolean): String = {
    val version: String = genType match {
      case GenType.Deployment => "apps/v1"
      case _                  => "v1"
    }
    val kind: String = genType match {
      case GenType.Service      => "Service"
      case GenType.Deployment   => "Deployment"
      case GenType.StorageClaim => "PersistentVolumeClaim"
      case GenType.StorageMount => "PersistentVolume"
    }
    val suffix = genType match {
      case GenType.StorageClaim => "-db-claim"
      case GenType.StorageMount => "-db-volume"
      case _ if isDb            => "-db"
      case _                    => ""
    }

    val name = service.name + suffix

    this.printer.pretty(Header(version, kind, Metadata(name, Labels(service.name, genType, isDb))).asJson)
  }

  private def generateDbStorage(service: Service): String = {
    val volumeBody = Body(
      PersistentVolumeSpec(
        storageClass = StorageClass.Manual,
        capacityGB = 1.0f,
        accessModes = Seq(AccessMode.ReadWriteMany),
        reclaimPolicy = ReclaimPolicy.Delete,
        hostPath = service.dbStorage.hostPath,
      ),
    ).asJson

    val claimBody = Body(
      PersistentVolumeClaimSpec(
        accessModes = Seq(AccessMode.ReadWriteMany),
        volumeName = s"${service.name}-db-volume",
        storageClassName = StorageClass.Manual,
        storageResourceRequestAmountMB = 100.0f,
      ),
    ).asJson

    mkCode(
      generateHeader(service, GenType.StorageMount, isDb = true),
      this.printer.pretty(volumeBody),
      "---\n",
      generateHeader(service, GenType.StorageClaim, isDb = true),
      this.printer.pretty(claimBody),
    )
  }

  private def generateDbService(service: Service): String = {
    val serviceBody = Body(
      ServiceSpec(
        ports = Seq(ServicePort("db", 5432, 5432)), //TODO: Make a Postgres data class that stores port info etc
        selector = Labels(service.name, GenType.Service, isDb = true),
      ),
    ).asJson
    mkCode(
      generateHeader(service, GenType.Service, isDb = true),
      this.printer.pretty(serviceBody),
    )
  }

  private def generateDbDeployment(service: Service): String = {
    val name = service.name + "-db"

    val dbContainer = Container(
      service.dbImage,
      name,
      ports = Seq(),
      env = service.dbEnvVars.map(EnvVar.tupled),
      volumeMounts = Seq(
        VolumeMount(service.dbStorage.dataMount, None, name + "-claim"),
        VolumeMount(service.dbStorage.initMount, Some(service.dbStorage.initFile), name + "-init"),
      ),
      lifecycle = Some(Lifecycle(service.dbLifecycleCommand)),
    )

    val podSpec = PodSpec(
      name,
      Seq(
        dbContainer,
      ),
      imagePullSecrets = Seq(),
      restartPolicy = RestartPolicy.Always,
      volumes = Seq(
        Volume(name + "-init", ConfigMap(name + "-config")),
        Volume(name + "-claim", PersistentVolume(name + "-claim")),
      ),
    )

    val deploymentBody = Body(
      DeploymentSpec(
        1,
        Selector(Labels(service.name, GenType.Deployment, isDb = true)),
        strategy = Some(Strategy(PlacementStrategy.Recreate)),
        Template(
          Metadata(name, Labels(service.name, GenType.Deployment, isDb = true)),
          podSpec,
        ),
      ),
    ).asJson

    mkCode(
      generateHeader(service, GenType.Deployment, isDb = true),
      this.printer.pretty(deploymentBody),
    )
  }

  private def generateService(service: Service): String = {
    val serviceBody = Body(
      ServiceSpec(
        service.ports.map { case name -> port => ServicePort(name, port, port) },
        Labels(service.name, GenType.Service, isDb = false),
      ),
    ).asJson
    mkCode(
      generateHeader(service, GenType.Service, isDb = false),
      this.printer.pretty(serviceBody),
    )
  }

  private def generateDeployment(service: Service): String = {

    val container = Container(
      service.image,
      service.name,
      service.ports.map { case (_, port) => ContainerPort(port) },
      env = service.appEnvVars.map(EnvVar.tupled),
      volumeMounts = Seq(),
    )

    val podSpec = PodSpec(
      service.name,
      Seq(
        container,
      ),
      Seq(Secret(service.secretName)),
      restartPolicy = RestartPolicy.Always,
      volumes = Seq(),
    )

    val deploymentBody = Body(
      DeploymentSpec(
        service.replicas,
        Selector(Labels(service.name, GenType.Deployment, isDb = false)),
        strategy = None,
        Template(
          Metadata(service.name, Labels(service.name, GenType.Deployment, isDb = false)),
          podSpec,
        ),
      ),
    ).asJson

    //Note: printer.pretty Adds a newline on the end of the string, so just mkCode suffices without .spaces
    mkCode(
      generateHeader(service, GenType.Deployment, isDb = false),
      this.printer.pretty(deploymentBody),
    )
  }

  private val kongFiles: Files = Map(
    File("kube/kong", "kong-db-deployment.yaml") -> FileUtils.readResources("kube/kong/kong-db-deployment.yaml"),
    File("kube/kong", "kong-db-service.yaml")    -> FileUtils.readResources("kube/kong/kong-db-service.yaml"),
    File("kube/kong", "kong-deployment.yaml")    -> FileUtils.readResources("kube/kong/kong-deployment.yaml"),
    File("kube/kong", "kong-migration-job.yaml") -> FileUtils.readResources("kube/kong/kong-migration-job.yaml"),
    File("kube/kong", "kong-service.yaml")       -> FileUtils.readResources("kube/kong/kong-service.yaml"),
  )

  private val deployFiles: Files = Map(
    File("kube/deploy", "deploy-daemon-set.yaml") -> FileUtils.readResources("kube/deploy/deploy-daemon-set.yaml"),
    File("kube/deploy", "deploy-replication-controller.yaml") -> FileUtils.readResources(
      "kube/deploy/deploy-replication-controller.yaml",
    ),
    File("kube/deploy", "deploy-service.yaml") -> FileUtils.readResources("kube/deploy/deploy-service.yaml"),
  )

  private def buildKubeFiles(service: Service) =
    Seq(
      File(s"kube/${service.name}", "deployment.yaml")    -> generateDeployment(service),
      File(s"kube/${service.name}", "service.yaml")       -> generateService(service),
      File(s"kube/${service.name}", "db-deployment.yaml") -> generateDbDeployment(service),
      File(s"kube/${service.name}", "db-service.yaml")    -> generateDbService(service),
      File(s"kube/${service.name}", "db-storage.yaml")    -> generateDbStorage(service),
    )

  /** Given an [[OrchestrationRoot]], check the services inside it and generate deployment scripts */
  def generate(orchestrationRoot: OrchestrationRoot): Files = {
    val kubeFiles: Files    = orchestrationRoot.services.flatMap(buildKubeFiles).toMap
    val kongConfig: Files   = Map(KongConfigGenerator.generate(orchestrationRoot))
    val deployScript: Files = Map(DeployScriptGenerator.generate(orchestrationRoot))
    kubeFiles ++ kongConfig ++ kongFiles ++ deployFiles ++ deployScript
  }

}
