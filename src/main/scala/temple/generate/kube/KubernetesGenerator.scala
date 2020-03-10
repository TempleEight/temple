package temple.generate.kube

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.syntax.AsYaml
import temple.generate.FileSystem._
import temple.generate.kube.ast.gen._
import temple.generate.kube.ast.{OrchestrationRoot, Service}
import temple.generate.utils.CodeTerm.mkCode

/** Generates the Kubernetes config files
  * for each microservice */
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

    Header(version, kind, Metadata(name, Labels(name, genType))).asJson.asYaml.spaces2
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

  private def generateDeployment(service: Service): String =
    generateHeader(service, GenType.Deployment, isDb = false)

  /** Given an [[OrchestrationRoot]], check the services inside it and generate deployment scripts */
  def generate(orchestrationRoot: OrchestrationRoot): Map[File, FileContent] = {
    var files: Map[File, FileContent] = Map()
    orchestrationRoot.services foreach { service =>
      files += (File(s"kube/${service.name}", "deployment.yaml")    -> generateDeployment(service))
      files += (File(s"kube/${service.name}", "service.yaml")       -> generateService(service))
      files += (File(s"kube/${service.name}", "db-deployment.yaml") -> generateDbDeployment(service))
      files += (File(s"kube/${service.name}", "db-service.yaml")    -> generateDbService(service))
      files += (File(s"kube/${service.name}", "db-storage.yaml")    -> generateDbStorage(service))
    }
    files
  }
}
