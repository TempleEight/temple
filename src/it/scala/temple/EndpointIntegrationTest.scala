package temple

import java.nio.file.{Files, Paths}

import org.scalatest.Matchers
import temple.containers.EndpointTesterSpec
import temple.utils.FileUtils

class EndpointIntegrationTest extends EndpointTesterSpec with Matchers {
  behavior of "temple generate"

  it should "correctly generate simple.temple" in {
    val simpleTemple = FileUtils.readFile("src/test/scala/temple/testfiles/simple-dc.temple")
    buildAndTestEndpoints(simpleTemple)
  }

  it should "correctly generate data.temple" in {
    val dataTemple = FileUtils.readFile("src/test/scala/temple/testfiles/data.temple")
    buildAndTestEndpoints(dataTemple)
  }

  it should "correctly generate the example Templefiles" in {
    Files
      .list(Paths.get("examples"))
      .map(path => path.toString)
      .filter((path: String) => path.endsWith(".temple"))
      .forEach { (path: String) =>
        val templefile = FileUtils.readFile(path)
        buildAndTestEndpoints(templefile)
      }
  }

  it should "correctly generate enumReadAll.temple" in {
    val enumReadAllTemple = FileUtils.readFile("src/test/scala/temple/testfiles/enumReadAll.temple")
    buildAndTestEndpoints(enumReadAllTemple)
  }
}
