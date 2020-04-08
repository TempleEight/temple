package temple.generate.orchestration.dockercompose

import temple.generate.FileSystem.{File, Files}
import temple.generate.orchestration.ast.OrchestrationType.OrchestrationRoot

object DockerComposeGenerator {

  def generate(projectName: String, orchestrationRoot: OrchestrationRoot): Files =
    Map(File("", "docker-compose.yml") -> "")
}
