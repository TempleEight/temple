package temple.generate.kube

import temple.generate.FileSystem.{File, FileContent}
import temple.generate.kube.ast.OrchestrationType.OrchestrationRoot
import temple.utils.StringUtils

object PushImageScriptGenerator {

  def generate(projectName: String, orchestrationRoot: OrchestrationRoot): (File, FileContent) = {
    val directories = orchestrationRoot.services
      .map { svc =>
        StringUtils.doubleQuote(svc.name)
      }
      .mkString(" ")

    File("", "push-image.sh") ->
    s"""#!/bin/sh
       |REGISTRY_URL="localhost:5000"
       |BASEDIR=$$(dirname "$$BASH_SOURCE")
       |
       |for service in $directories; do
       |  docker build -t "$$REGISTRY_URL/${StringUtils.kebabCase(projectName)}-$$service" $$BASEDIR/$$service
       |  docker push "$$REGISTRY_URL/${StringUtils.kebabCase(projectName)}-$$service"
       |done
       |""".stripMargin
  }
}
