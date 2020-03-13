package temple.generate.kube

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.Printer
import io.circe.yaml.syntax.AsYaml
import temple.generate.FileSystem._
import temple.generate.kube.ast.OrchestrationType._
import temple.generate.kube.ast.gen.KubeType._
import temple.generate.kube.ast.gen.Spec._
import temple.generate.utils.CodeTerm.mkCode

/** Generates the Kubernetes config files for each microservice */
object KubernetesGenerator {

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
    val name = service.name + { if (isDb) "-db" else "" }

    Header(version, kind, Metadata(name, Labels(service.name, genType, isDb))).asJson.asYaml.spaces2
  }

  private def generateDbStorage(service: Service): String =
    mkCode.lines(
      generateHeader(service, GenType.StorageMount, isDb = true),
      "---",
      generateHeader(service, GenType.StorageClaim, isDb = true),
    )

  private def generateDbService(service: Service): String =
    generateHeader(service, GenType.Service, isDb = true)

  private def generateDbDeployment(service: Service): String = {
    val name = service.name + "-db"
    val deploymentBody = Body(
      DeploymentSpec(
        1,
        Selector(Labels(service.name, GenType.Deployment, isDb = true)),
        strategy = Some(Strategy("Recreate")),
        Template(
          Metadata(name, Labels(service.name, GenType.Deployment, isDb = true)),
          PodSpec(
            name,
            Seq(
              Container(
                service.dbImage,
                name,
                ports = Seq(),
                env = service.envVars.map(x => EnvVar(x._1, x._2)),
                volumeMounts = Seq(
                  VolumeMount("/var/lib/postgresql/data", None, name + "-claim"),
                  VolumeMount("/docker-entrypoint-initdb.d/init.sql", Some("init.sql"), name + "-init"),
                ),
                lifecycle = Some(Lifecycle("echo done")),
              ),
            ),
            imagePullSecrets = Seq(),
            restartPolicy = "Always",
            volumes = Seq(
              Volume(name + "-init", ConfigMap(name + "-config")),
              Volume(name + "-claim", PersistentVolume(name + "-claim")),
            ),
          ),
        ),
      ),
    ).asJson
    val printer = Printer(preserveOrder = true, dropNullKeys = true)
    mkCode(
      generateHeader(service, GenType.Deployment, isDb = true),
      printer.pretty(deploymentBody),
    )
  }

  private def generateService(service: Service): String = {
    val serviceBody = Body(
      ServiceSpec(
        service.ports.map { case name -> port => ServicePort(name, port, port) },
        Labels(service.name, GenType.Service, isDb = false),
      ),
    ).asJson.asYaml.spaces2
    mkCode(
      generateHeader(service, GenType.Service, isDb = false),
      serviceBody,
    )
  }

  private def generateDeployment(service: Service): String = {
    val deploymentBody = Body(
      DeploymentSpec(
        service.replicas,
        Selector(Labels(service.name, GenType.Deployment, isDb = false)),
        strategy = None,
        Template(
          Metadata(service.name, Labels(service.name, GenType.Deployment, isDb = false)),
          PodSpec(
            service.name,
            Seq(
              Container(
                service.image,
                service.name,
                service.ports.map(x => ContainerPort(x._2)),
                env = Seq(),
                volumeMounts = Seq(),
              ),
            ),
            Seq(Secret(service.secretName)),
            restartPolicy = "Always",
            volumes = Seq(),
          ),
        ),
      ),
    ).asJson.asYaml.spaces2
    //Note: .spaces2 Adds a newline on the end of the string, so just mkCode suffices without .spaces
    mkCode(
      generateHeader(service, GenType.Deployment, isDb = false),
      deploymentBody,
    )
  }

  /** Given an [[OrchestrationRoot]], check the services inside it and generate deployment scripts */
  def generate(orchestrationRoot: OrchestrationRoot): Map[File, FileContent] =
    orchestrationRoot.services.flatMap { service =>
      Seq(
        File(s"kube/${service.name}", "deployment.yaml")    -> generateDeployment(service),
        File(s"kube/${service.name}", "service.yaml")       -> generateService(service),
        File(s"kube/${service.name}", "db-deployment.yaml") -> generateDbDeployment(service),
        File(s"kube/${service.name}", "db-service.yaml")    -> generateDbService(service),
        File(s"kube/${service.name}", "db-storage.yaml")    -> generateDbStorage(service),
      )
    }.toMap
}
