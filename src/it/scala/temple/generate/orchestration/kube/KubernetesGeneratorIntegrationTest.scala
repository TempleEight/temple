package temple.generate.orchestration.kube

import org.scalatest.{BeforeAndAfter, Matchers}
import temple.containers.KubeSpec
import temple.generate.FileSystem.File

class KubernetesGeneratorIntegrationTest extends KubeSpec with Matchers with BeforeAndAfter {

  behavior of "KubeSpec"

  it should "fail on an invalid docker spec" in {
    val results = validateAll(
      Map(
        File("test", "test.yaml") -> "asdf",
      ),
      File("test", ""),
    ).split('\n')

    results foreach { result =>
      result should startWith("ERR")
    }
  }

  behavior of "KubernetesGeneratorIntegrationTest"

  it should "generate valid simple services" in {
    val results = validateAll(
      KubernetesGenerator.generate("example", KubernetesGeneratorIntegrationTestData.basicOrchestrationRoot),
      File("kube/user", ""),
    ).split('\n')

    results foreach { result =>
      result should startWith("PASS")
    }
  }

}
