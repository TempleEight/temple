package temple.generate.service

import org.scalatest.{BeforeAndAfter, Matchers}
import temple.containers.GolangSpec
import temple.generate.FileSystem._
import temple.generate.service.go.GoServiceGenerator

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
}
