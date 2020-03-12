package temple.generate.kube

import io.circe.generic.auto._
import io.circe.syntax._
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

    Header(version, kind, Metadata(name, Labels(name, genType, isDb))).asJson.asYaml.spaces2
  }

  private def generateDbStorage(service: Service): String =
    mkCode.lines(
      generateHeader(service, GenType.StorageMount, isDb = true),
      "---",
      generateHeader(service, GenType.StorageClaim, isDb = true),
    )

  private def generateDbService(service: Service): String =
    generateHeader(service, GenType.Service, isDb = true)

  private def generateDbDeployment(service: Service): String =
    generateHeader(service, GenType.Deployment, isDb = true)

  private def generateService(service: Service): String =
    generateHeader(service, GenType.Service, isDb = false)

  private def generateDeployment(service: Service): String = {
    val deploymentBody = Body(
      DeploymentSpec(
        service.replicas,
        Selector(Labels(service.name, GenType.Deployment, isDb = false)),
        Template(
          Metadata(service.name, Labels(service.name, GenType.Deployment, isDb = false)),
          PodSpec(
            service.name,
            Seq(Container(service.image, service.name, service.ports.map(Port))),
            Seq(Secret(service.secretName)),
            restartPolicy = "Always",
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
