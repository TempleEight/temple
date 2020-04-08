package temple.generate.orchestration.dockercompose

import io.circe.yaml.Printer
import io.circe.syntax._
import temple.generate.FileSystem.{File, Files}
import temple.generate.orchestration.ast.OrchestrationType
import temple.generate.orchestration.ast.OrchestrationType.OrchestrationRoot
import temple.generate.orchestration.dockercompose.ast.Service.{ExternalService, LocalService}
import temple.generate.orchestration.dockercompose.ast.{DockerComposeRoot, Service}
import Option.when

object DockerComposeGenerator {

  def generateService(service: OrchestrationType.Service): (String, Service) =
    service.name -> LocalService(
      path = s"./${service.name}",
      ports = service.ports.map(_._2),
      networks = Seq(s"${service.name}-network", "kong-network"),
    )

  def generateDatabaseService(service: OrchestrationType.Service): (String, Service) =
    s"${service.name}-db" -> ExternalService(
      image = service.dbImage,
      environment = service.dbEnvVars,
      volumes = Seq(s"./${service.name}-db/${service.dbStorage.initFile}" -> service.dbStorage.initMount),
      ports = Seq(),
      networks = Seq(s"${service.name}-network"),
    )

  def generate(projectName: String, orchestrationRoot: OrchestrationRoot): Files = {
    var services: Map[String, Service] = orchestrationRoot.services.flatMap { service =>
      Seq(generateService(service), generateDatabaseService(service))
    }.toMap

    val serviceNetworks: Seq[String] = orchestrationRoot.services.map { svc =>
      s"${svc.name}-network"
    }

    if (orchestrationRoot.usesMetrics) {
      // TODO: These strings should come from somewhere else - move them with Kube!
      services += ("grafana" -> ExternalService(
        image = "grafana/grafana:6.6.2",
        environment = Seq(),
        volumes = Seq("./grafana/provisioning" -> "/etc/grafana/provisioning"),
        ports = Seq(3000),
        networks = Seq("metrics-network"),
      ))

      services += ("prometheus" -> ExternalService(
        image = "prom/prometheus:v2.16.0",
        environment = Seq(),
        volumes = Seq("./prometheus" -> "/etc/prometheus"),
        ports = Seq(9090),
        networks = serviceNetworks :+ "metrics-network",
      ))
    }

    val allNetworks: Seq[String] = (serviceNetworks :+ "kong-network") ++ when(orchestrationRoot.usesMetrics) {
        "metrics-network"
      }

    val composeRoot = DockerComposeRoot(services, allNetworks)
    val yaml        = Printer(preserveOrder = true).pretty(composeRoot.asJson)
    Map(File("", "docker-compose.yml") -> yaml)
  }
}
