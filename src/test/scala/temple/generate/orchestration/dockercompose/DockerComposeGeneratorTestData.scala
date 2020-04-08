package temple.generate.orchestration.dockercompose

import temple.utils.FileUtils

object DockerComposeGeneratorTestData {
  val dockerComposeWithoutMetrics = FileUtils.readResources("docker-compose/docker-compose.yml")
  val dockerComposeWithMetrics    = FileUtils.readResources("docker-compose/docker-compose-metrics.yml")
}
