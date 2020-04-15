package temple.generate.orchestration.kube

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.Printer
import temple.builder.project.ProjectConfig
import temple.generate.FileSystem.{File, Files}
import temple.generate.orchestration.ast.OrchestrationType.OrchestrationRoot
import temple.generate.orchestration.kube.ast.KubeType.{Body, Header, Labels, Metadata}
import temple.generate.orchestration.kube.ast.RestartPolicy
import temple.generate.orchestration.kube.ast.Spec._
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.FileUtils

/**
  * Generates Kubernetes Services and Deployments for running Prometheus and Grafana.
  * The only one of these files that isn't static between applications is the Grafana deployment.
  */
object KubernetesMetricGenerator {

  private val printer = Printer.spaces2

  private def generateGrafanaDeployment(orchestrationRoot: OrchestrationRoot): String = {

    val header =
      Header("apps/v1", "Deployment", Metadata("grafana", Labels("grafana", GenType.None, isDb = false))).asJson

    val volumeMounts = Seq(
        VolumeMount(
          "/etc/grafana/provisioning/datasources/datasource.yml",
          Some("datasource.yml"),
          "grafana-datasource",
        ),
        VolumeMount(
          "/etc/grafana/provisioning/dashboards/dashboards.yml",
          Some("dashboards.yml"),
          "grafana-dashboards",
        ),
      ) ++ orchestrationRoot.services.map { service =>
        VolumeMount(
          s"/etc/grafana/provisioning/dashboards/${service.name}.json",
          subPath = Some(s"${service.name}.json"),
          name = s"grafana-${service.name}",
        )
      }

    val volumes = Seq(
        Volume("grafana-datasource", ConfigMap("grafana-datasource-config")),
        Volume("grafana-dashboards", ConfigMap("grafana-dashboards-config")),
      ) ++ orchestrationRoot.services.map { service =>
        Volume(s"grafana-${service.name}", ConfigMap(s"grafana-${service.name}-config"))
      }

    val container = Container(
      ProjectConfig.grafanaDockerImage.toString,
      "grafana",
      ports = Seq(ContainerPort(3000)),
      env = Seq(),
      volumeMounts = volumeMounts,
    )

    val body = Body(
      DeploymentSpec(
        replicas = 1,
        Selector(Labels("grafana", GenType.None, isDb = false)),
        strategy = None,
        Template(
          Metadata("grafana", Labels("grafana", GenType.None, isDb = false)),
          PodSpec(
            "grafana",
            containers = Seq(
              container,
            ),
            imagePullSecrets = Seq(),
            restartPolicy = RestartPolicy.Always,
            volumes = volumes,
          ),
        ),
      ),
    ).asJson

    mkCode(
      printer.pretty(header),
      printer.pretty(body),
    )
  }

  // Given an OrchestrationRoot that uses metrics, generate the required prom and grafana kube files
  def generate(orchestrationRoot: OrchestrationRoot): Files = Map(
    File("kube/prom", "prometheus-deployment.yaml") -> FileUtils.readResources(
      "kube/metric/prometheus-deployment.yaml",
    ),
    File("kube/prom", "prometheus-service.yaml")    -> FileUtils.readResources("kube/metric/prometheus-service.yaml"),
    File("kube/grafana", "grafana-service.yaml")    -> FileUtils.readResources("kube/metric/grafana-service.yaml"),
    File("kube/grafana", "grafana-deployment.yaml") -> generateGrafanaDeployment(orchestrationRoot),
  )
}
