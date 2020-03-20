package temple.generate.server.go

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.server.go.service.GoServiceGenerator
import temple.generate.FileSystem._

class GoServiceGeneratorTest extends FlatSpec with Matchers {

  behavior of "GoServiceGenerator"

  it should "generate simple services correctly" in {
    GoServiceGenerator.generate(GoServiceGeneratorTestData.simpleServiceRoot) shouldBe GoServiceGeneratorTestData.simpleServiceFiles
  }

  it should "generate simple services with inter-service communication correctly" in {
    GoServiceGenerator.generate(GoServiceGeneratorTestData.simpleServiceRootWithComms)(File("match/dao", "dao.go")) shouldBe GoServiceGeneratorTestData
      .simpleServiceFilesWithComms(File("match/dao", "dao.go"))
  }
}
