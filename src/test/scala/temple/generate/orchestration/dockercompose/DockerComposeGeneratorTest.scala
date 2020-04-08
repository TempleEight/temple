package temple.generate.orchestration.dockercompose

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.orchestration.UnitTestData

class DockerComposeGeneratorTest extends FlatSpec with Matchers {

  behavior of "DockerComposeGenerator"

  it should "generate an empty file" in {
    val generated = DockerComposeGenerator.generate("example", UnitTestData.basicOrchestrationRootWithMetrics)
    generated.head._2 shouldBe ""
  }
}
