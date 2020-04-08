package temple.generate.orchestration.dockercompose

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.orchestration.UnitTestData

class DockerComposeGeneratorTest extends FlatSpec with Matchers {

  behavior of "DockerComposeGenerator"

  it should "generate a docker-compose file for a service without metrics" in {
    val (_, fileContents) =
      DockerComposeGenerator.generate("example", UnitTestData.basicOrchestrationRootWithoutMetrics).head
    fileContents shouldBe DockerComposeGeneratorTestData.dockerComposeWithoutMetrics
  }

  it should "generate a docker-compose file for a service with metrics" in {
    val (_, fileContents) =
      DockerComposeGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics).head
    fileContents shouldBe DockerComposeGeneratorTestData.dockerComposeWithMetrics
  }
}
