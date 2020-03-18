package temple.generate.service.go.auth

import org.scalatest.{FlatSpec, Matchers}

class GoAuthServiceGeneratorTest extends FlatSpec with Matchers {

  behavior of "GoAuthServiceGenerator"

  it should "generate auth services correctly" in {
    GoAuthServiceGenerator.generate(GoAuthServiceGeneratorTestData.authServiceRoot) shouldBe GoAuthServiceGeneratorTestData.authServiceFiles
  }
}
