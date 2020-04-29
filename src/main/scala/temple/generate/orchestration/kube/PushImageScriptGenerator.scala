package temple.generate.orchestration.kube

import temple.generate.FileSystem.{File, FileContent}
import temple.generate.orchestration.ast.OrchestrationType.OrchestrationRoot
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
       |REG_URL="$${REG_URL:-localhost:5000}"
       |BASEDIR=$$(dirname "$$BASH_SOURCE")
       |
       |for service in $directories; do
       |  docker build -t "$$REG_URL/${StringUtils.kebabCase(projectName)}-$$service" "$$BASEDIR/$$service"
       |  docker push "$$REG_URL/${StringUtils.kebabCase(projectName)}-$$service"
       |done
       |""".stripMargin
  }
}
