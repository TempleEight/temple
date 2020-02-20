package temple.generate.docker

import org.scalatest.{BeforeAndAfter, Matchers}
import temple.containers.HadolintSpec

class DockerGeneratorIntegrationTest extends HadolintSpec with Matchers with BeforeAndAfter {

  behavior of "DockerValidator"
  it should "succeed when a versioned tag is used" in {
    validate("FROM golang:123\nEXPOSE 80") shouldBe empty
  }

  it should "fail when a latest tag is used" in {
    validate("FROM golang:latest\nEXPOSE 80") should include(Rules.useLatest)
  }
}
