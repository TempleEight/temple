package temple.generate.kube

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.FileSystem.File

class KongConfigGeneratorTest extends FlatSpec with Matchers {

  behavior of "KongConfigGeneratorTest"

  it should "generate correct kong configs" in {
    val (outputFile, outputContents) = KongConfigGenerator.generate(UnitTestData.basicOrchestrationRootWithoutMetrics)
    val file                         = File("kong", "configure-kong.sh")
    outputFile should be(file)
    outputContents should be(UnitTestData.userKongConfig)
  }

}
