package temple.generate.kube

import temple.generate.FileSystem.{File, FileContent}
import temple.generate.kube.ast.OrchestrationType.OrchestrationRoot
import temple.utils.StringUtils

object PushImageScriptGenerator {

  def generate(projectName: String, orchestrationRoot: OrchestrationRoot): (File, FileContent) =
    File("", "push-image.sh") ->
    s"""#!/bin/sh
       |REGISTRY_URL="localhost:5000"
       |
       |for service in ${orchestrationRoot.services.map(_.name).map(StringUtils.doubleQuote).mkString(" ")}; do
       |  docker build -t "$$REGISTRY_URL/${StringUtils.kebabCase(projectName)}-$$service" $$service
       |  docker push "$$REGISTRY_URL/${StringUtils.kebabCase(projectName)}-$$service"
       |done
       |""".stripMargin
}
