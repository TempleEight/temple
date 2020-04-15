package temple.generate.orchestration.dockercompose

import io.circe.syntax._
import io.circe.yaml.Printer
import temple.ast.Metadata.Provider
import temple.generate.FileSystem.{File, Files}
import temple.generate.orchestration.KongConfigGenerator
import temple.generate.orchestration.ast.OrchestrationType
import temple.generate.orchestration.ast.OrchestrationType.OrchestrationRoot
import temple.generate.orchestration.dockercompose.ast.Service.{ExternalService, LocalService}
import temple.generate.orchestration.dockercompose.ast.kong.{KongDatabase, KongMigrations, KongService}
import temple.generate.orchestration.dockercompose.ast.{DockerComposeRoot, Service}
import temple.generate.orchestration.kube.DeployScriptGenerator

import scala.Option.when
import scala.collection.immutable.ListMap

object DockerComposeGenerator {

  def generateService(service: OrchestrationType.Service): (String, Service) =
    service.name -> LocalService(
      path = s"./${service.name}",
      ports = Seq(service.ports.service, service.ports.metrics),
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
    var services: ListMap[String, Service] = ListMap(
        "kong"            -> KongService,
        "kong-db"         -> KongDatabase,
        "kong-migrations" -> KongMigrations,
      ) ++ orchestrationRoot.services.flatMap { service =>
        Seq(generateService(service), generateDatabaseService(service))
      }

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
    val networksMap: Map[String, Map[String, String]] = allNetworks.map(name => (name, Map[String, String]())).toMap

    val composeRoot = DockerComposeRoot(services, networksMap)
    val yaml        = Printer(preserveOrder = true).pretty(composeRoot.asJson)
    Map(
      File("", "docker-compose.yml") -> yaml,
      KongConfigGenerator.generate(orchestrationRoot),
      DeployScriptGenerator.generate(orchestrationRoot, Provider.DockerCompose),
    )
  }
}
