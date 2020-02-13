package temple.generate.docker

import org.scalatest.{FlatSpec, Matchers}

class DockerFileGeneratorTest extends FlatSpec with Matchers {

  "DockerFileGenerator" should "generate correct base statements" in {
    DockerFileGenerator.generate(UnitTestData.basicDockerFileRoot) shouldBe UnitTestData.basicDockerFileString
  }

}
