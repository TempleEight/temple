package temple.generate.service.go

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.service.go.auth.GoAuthServiceGenerator

class GoAuthServiceGeneratorTest extends FlatSpec with Matchers {

  behavior of "GoAuthServiceGenerator"

  it should "generate auth services correctly" in {
    GoAuthServiceGenerator.generate(GoAuthServiceGeneratorTestData.authServiceRoot) shouldBe GoAuthServiceGeneratorTestData.authServiceFiles
  }
}
