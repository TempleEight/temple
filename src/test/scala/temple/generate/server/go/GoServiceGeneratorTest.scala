package temple.generate.server.go

import org.scalatest.FlatSpec
import temple.generate.FileMatchers
import temple.generate.server.go.service.GoServiceGenerator

class GoServiceGeneratorTest extends FlatSpec with FileMatchers {

  behavior of "GoServiceGenerator"

  it should "generate simple services correctly" in {
    filesShouldMatch(
      GoServiceGenerator.generate(GoServiceGeneratorTestData.simpleServiceRoot),
      GoServiceGeneratorTestData.simpleServiceFiles,
    )
  }

  it should "generate simple services with inter-service communication correctly" in {
    filesShouldMatch(
      GoServiceGenerator.generate(GoServiceGeneratorTestData.simpleServiceRootWithComms),
      GoServiceGeneratorTestData.simpleServiceFilesWithComms,
    )
  }
}
