package temple.generate.kube

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.FileSystem.File

class KongConfigGeneratorTest extends FlatSpec with Matchers {

  behavior of "KongConfigGeneratorTest"

  it should "generate correct kong configs" in {
    val output = KongConfigGenerator.generate(UnitTestData.basicOrchestrationRoot)
    val file   = File("kong", "configure-kong.sh")
    output._1 should be(file)
    output._2 should be(UnitTestData.userKongConfig)
  }

}
