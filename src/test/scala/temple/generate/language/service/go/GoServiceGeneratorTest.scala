package temple.generate.language.service.go

import org.scalatest.{FlatSpec, Matchers}

class GoServiceGeneratorTest extends FlatSpec with Matchers {

  behavior of "GoServiceGenerator"

  it should "generate simple services correctly" in {
    GoServiceGenerator.generate(GoServiceGeneratorTestData.simpleServiceRoot) shouldBe GoServiceGeneratorTestData.simpleServiceFiles
  }

  it should "generate simple services with inter-service communication correctly" in {
    GoServiceGenerator.generate(GoServiceGeneratorTestData.simpleServiceRootWithComms) shouldBe GoServiceGeneratorTestData.simpleServiceFilesWithComms
  }
}
