package temple.generate.kube

import temple.generate.FileSystem.{File, FileContent}
import temple.generate.kube.ast.OrchestrationType.OrchestrationRoot
import temple.generate.utils.CodeTerm.mkCode
import temple.utils.FileUtils

object DeployScriptGenerator {

  private val scriptTemplateHeader: String = FileUtils.readResources("shell/deploy.sh.header.snippet")

  private val scriptTemplateBody: String = FileUtils.readResources("shell/deploy.sh.body.snippet")

  def generate(orchestrationRoot: OrchestrationRoot): (File, FileContent) = File("", "deploy.sh") -> mkCode.lines(
    scriptTemplateHeader,
    "",
    "# DB init scripts",
    orchestrationRoot.services.map { service =>
      s"""kubectl create configmap ${service.name}-db-config --from-file "$$BASEDIR/${service.name}-db/init.sql" -o=yaml"""
    },
    "",
    scriptTemplateBody,
  )
}
