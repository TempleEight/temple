package temple.generate.language.service

import org.scalatest.{BeforeAndAfter, Matchers}
import temple.containers.GolangSpec
import temple.utils.FileUtils

class GoGeneratorIntegrationTest extends GolangSpec with Matchers with BeforeAndAfter {

  behavior of "GolangValidator"

  it should "fail when an empty file is provided" in {
    val validationErrors = validate("")
    validationErrors should not be empty
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
    validationErrors shouldBe empty
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
        FileUtils.File("sample-proj", "main.go")      -> sampleMain,
        FileUtils.File("sample-proj/pkg2", "pkg2.go") -> sampleOtherPkg,
        FileUtils.File("sample-proj", "go.mod")       -> goMod,
      ),
      FileUtils.File("sample-proj", "main.go"),
    )

    validationErrors shouldBe empty
  }
}
