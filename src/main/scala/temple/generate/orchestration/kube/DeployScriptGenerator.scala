package temple.generate.orchestration.kube

import temple.ast.Metadata
import temple.generate.FileSystem.{File, FileContent}
import temple.generate.orchestration.ast.OrchestrationType.OrchestrationRoot
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.FileUtils

object DeployScriptGenerator {

  private val scriptTemplateHeader: String = FileUtils.readResources("shell/deploy.sh.header.snippet")

  private val scriptTemplateBody: String = FileUtils.readResources("shell/deploy.sh.body.snippet")

  private val scriptMetricsBlock: String = FileUtils.readResources("shell/deploy.sh.metrics.snippet")

  private val scriptTemplateDeployment: String = FileUtils.readResources("shell/deploy.sh.deployment.snippet")

  def generate(orchestrationRoot: OrchestrationRoot, provider: Metadata.Provider): (File, FileContent) =
    File("", "deploy.sh") -> (provider match {
      case Metadata.Provider.Kubernetes =>
        mkCode.lines(
          scriptTemplateHeader,
          "# DB init scripts",
          orchestrationRoot.services.map { service =>
            s"""kubectl create configmap ${service.name}-db-config --from-file "$$BASEDIR/${service.name}-db/init.sql" -o=yaml"""
          },
          "",
          Option.when(orchestrationRoot.usesMetrics) { scriptMetricsBlock },
          scriptTemplateDeployment,
          scriptTemplateBody,
        )
      case Metadata.Provider.DockerCompose =>
        FileUtils.readResources("shell/deploy-docker-compose.sh")
    })
}
