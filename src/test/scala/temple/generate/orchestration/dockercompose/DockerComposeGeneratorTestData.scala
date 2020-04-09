package temple.generate.orchestration.dockercompose

import temple.utils.FileUtils

object DockerComposeGeneratorTestData {
  val dockerComposeWithoutMetrics: String = FileUtils.readResources("docker-compose/docker-compose.yml")
  val dockerComposeWithMetrics: String    = FileUtils.readResources("docker-compose/docker-compose-metrics.yml")
  val dockerComposeDeployScript: String   = FileUtils.readResources("docker-compose/deploy.sh")
}
