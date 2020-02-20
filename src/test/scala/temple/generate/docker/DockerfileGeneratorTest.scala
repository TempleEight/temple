package temple.generate.docker

import org.scalatest.{FlatSpec, Matchers}

class DockerfileGeneratorTest extends FlatSpec with Matchers {

  behavior of "DockerfileGenerator"

  it should "generate correct base statements" in {
    DockerfileGenerator.generate(UnitTestData.basicDockerfileRoot) shouldBe UnitTestData.basicDockerfileString
  }

}
