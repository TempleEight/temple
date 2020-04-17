package temple.generate.server

import org.scalatest.{BeforeAndAfter, Matchers}
import temple.DSL.DSLProcessor
import temple.DSL.semantics.Analyzer
import temple.builder.project.ProjectBuilder
import temple.containers.GolangSpec
import temple.detail.LanguageDetail.GoLanguageDetail
import temple.generate.FileSystem._
import temple.generate.server.go.auth.GoAuthServiceGenerator
import temple.generate.server.go.service.GoServiceGenerator
import temple.utils.{FileUtils, StringUtils}
import temple.utils.MonadUtils.FromEither

class GoGeneratorIntegrationTest extends GolangSpec with Matchers with BeforeAndAfter {

  behavior of "GolangValidator"

  it should "fail when an empty file is provided" in {
    val validationErrors = validate("")
    validationErrors should not be ""
  }

  it should "succeed when a sample Go file is validated" in {
    val sampleFile =
      """package main
        |
        |import "fmt"
        |
        |func main() {
        |   fmt.Println("Hello World!")
        |}
        |""".stripMargin
    val validationErrors = validate(sampleFile)
    validationErrors shouldBe ""
  }

  it should "succeed when referencing other files" in {
    val sampleMain =
      """package main
        |
        |import (
        |    "fmt"
        |    "example.com/sample/pkg2"
        |)
        |
        |func main() {
        |    fmt.Printf("My favourite number is %d", pkg2.MyFavouriteNumber)
        |}
        |""".stripMargin

    val sampleOtherPkg =
      """package pkg2 
        |
        |var MyFavouriteNumber = 42
        |""".stripMargin

    val goMod =
      """module example.com/sample
        |
        |go 1.13
        |""".stripMargin

    val validationErrors = validateAll(
      Map(
        File("sample-proj", "main.go")      -> sampleMain,
        File("sample-proj/pkg2", "pkg2.go") -> sampleOtherPkg,
        File("sample-proj", "go.mod")       -> goMod,
      ),
      File("sample-proj", "main.go"),
    )

    validationErrors shouldBe ""
  }

  behavior of "GoServiceGenerator"

  it should "generate compilable simple services" in {
    val validationErrors = validateAll(
      GoServiceGenerator.generate(GoGeneratorIntegrationTestData.simpleServiceRoot),
      File("user", "user.go"),
    )

    validationErrors shouldBe ""
  }

  it should "generate compilable simple services with inter-service communication" in {
    val validationErrors = validateAll(
      GoServiceGenerator.generate(GoGeneratorIntegrationTestData.simpleServiceRootWithComms),
      File("match", "match.go"),
    )

    validationErrors shouldBe ""
  }

  it should "generate a compilable service when only a datetime attribute is used" in {
    val validationErrors = validateAll(
      GoServiceGenerator.generate(GoGeneratorIntegrationTestData.datetimeService),
      File("date-time-svc", "date-time-svc.go"),
    )

    validationErrors shouldBe ""
  }

  it should "generate a compilable service with an unbounded blob attribute" in {
    val validationErrors = validateAll(
      GoServiceGenerator.generate(GoGeneratorIntegrationTestData.unboundedBlob),
      File("unbounded-blob-svc", "unbounded-blob-svc.go"),
    )

    validationErrors shouldBe ""
  }

  it should "generate a compilable service with a bounded blob attribute" in {
    val validationErrors = validateAll(
      GoServiceGenerator.generate(GoGeneratorIntegrationTestData.boundedBlob),
      File("bounded-blob-svc", "bounded-blob-svc.go"),
    )

    validationErrors shouldBe ""
  }

  it should "generate a compilable auth service when metrics are not used" in {
    val validationErrors = validateAll(
      GoAuthServiceGenerator.generate(GoGeneratorIntegrationTestData.authNoMetricsService),
      File("auth", "auth.go"),
    )

    validationErrors shouldBe ""
  }

  it should "generate the entire simple.temple project" in {
    val simpleTemple = FileUtils.readFile("src/test/scala/temple/testfiles/simple.temple")
    val parsed = DSLProcessor.parse(simpleTemple) fromEither { error =>
      throw new RuntimeException(error)
    }
    val analyzedTemplefile = Analyzer.parseAndValidate(parsed)
    val detail             = GoLanguageDetail("example.com")
    val project            = ProjectBuilder.build(analyzedTemplefile, detail)

    analyzedTemplefile.allServices.keys.map(StringUtils.kebabCase).foreach { service =>
      val filesInService = project.files.filter { case (file, _) => file.folder.startsWith(service) }
      val validationErrors = validateAll(
        filesInService,
        entryFile = File(service, s"$service.go"),
      )

      validationErrors shouldBe empty
    }
  }
}
