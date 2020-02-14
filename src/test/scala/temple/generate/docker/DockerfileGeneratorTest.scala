package temple.generate.docker

import org.scalatest.{FlatSpec, Matchers}

class DockerfileGeneratorTest extends FlatSpec with Matchers {

  "DockerfileGenerator" should "generate correct base statements" in {
    DockerfileGenerator.generate(UnitTestData.basicDockerfileRoot) shouldBe UnitTestData.basicDockerfileString
  }

}
