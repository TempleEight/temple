package temple

import java.nio.file.{Files, Paths}
import java.util.function.Predicate

import org.scalatest.Matchers
import temple.containers.EndpointTesterSpec
import temple.utils.FileUtils

class EndpointIntegrationTest extends EndpointTesterSpec with Matchers {
  behavior of "temple generate"

  it should "correctly generate simple.temple" in {
    val simpleTemple = FileUtils.readFile("src/test/scala/temple/testfiles/simple-dc.temple")
    noException should be thrownBy buildAndTestEndpoints(simpleTemple)
  }

  it should "correctly generate the example Templefiles" in {
    Files
      .list(Paths.get("examples"))
      .map(path => path.toString)
      .filter((path: String) => path.endsWith(".temple"))
      .forEach { (path: String) =>
        val templefile = FileUtils.readFile(path)
        noException should be thrownBy buildAndTestEndpoints(templefile)
      }
  }

  it should "correctly generate enumReadAll.temple" in {
    val enumReadAllTemple = FileUtils.readFile("src/test/scala/temple/testfiles/enumReadAll.temple")
    noException should be thrownBy buildAndTestEndpoints(enumReadAllTemple)
  }
}
