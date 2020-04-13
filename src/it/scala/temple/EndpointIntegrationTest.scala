package temple

import org.scalatest.Matchers
import temple.containers.EndpointTesterSpec
import temple.utils.FileUtils

class EndpointIntegrationTest extends EndpointTesterSpec with Matchers {
  behavior of "temple generate"

  it should "correctly generate simple.temple" in {
    val simpleTemple = FileUtils.readFile("src/test/scala/temple/testfiles/simple-dc.temple")
    noException should be thrownBy buildAndTestEndpoints(simpleTemple)
  }
}
