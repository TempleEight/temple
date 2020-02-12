package temple.generate.docker

import org.scalatest.{FlatSpec, Matchers}

class DockerFileGeneratorTest extends FlatSpec with Matchers {

  behavior of "DockerFileGeneratorTest"

  "DockerFileGenerator" should "generate correct base statements" in {
    DockerFileGenerator.generate(TestData.basicDockerFileRoot) shouldBe TestData.basicDockerFileString
  }

}
