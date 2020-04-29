package temple.generate.server.go

import org.scalatest.FlatSpec
import temple.generate.FileMatchers
import temple.generate.server.go.auth.GoAuthServiceGenerator

class GoAuthServiceGeneratorTest extends FlatSpec with FileMatchers {

  behavior of "GoAuthServiceGenerator"

  it should "generate auth services correctly" in {
    filesShouldMatch(
      GoAuthServiceGenerator.generate(GoAuthServiceGeneratorTestData.authServiceRoot),
      GoAuthServiceGeneratorTestData.authServiceFiles,
    )
  }
}
