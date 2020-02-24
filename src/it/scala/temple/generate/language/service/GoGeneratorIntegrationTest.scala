package temple.generate.language.service

import org.scalatest.{BeforeAndAfter, Matchers}
import temple.containers.GolangSpec
import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement._

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
}
