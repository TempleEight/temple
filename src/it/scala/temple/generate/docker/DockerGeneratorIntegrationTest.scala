package temple.generate.docker

import org.scalatest.{BeforeAndAfter, Matchers}
import temple.ast.Metadata.Provider
import temple.ast.{ProjectBlock, Templefile}
import temple.builder.DockerfileBuilder
import temple.containers.HadolintSpec
import temple.generate.docker.ast.DockerfileRoot
import temple.generate.docker.ast.Statement._

class DockerGeneratorIntegrationTest extends HadolintSpec with Matchers with BeforeAndAfter {

  behavior of "DockerValidator"

  it should "succeed when a versioned tag is used" in {
    validate("FROM golang:123\nEXPOSE 80") shouldBe empty
  }

  it should "fail when a latest tag is used" in {
    validate("FROM golang:latest\nEXPOSE 80") should include(Rules.useLatest)
  }

  behavior of "DockerfileGenerator"

  it should "generate valid Dockerfile" in {
    val content = DockerfileRoot(
      From("golang", Some("123")),
      Seq(
        Run("echo", Seq("foo", "bar", "baz")),
        Cmd("cat", Seq("foo", "bar", "baz")),
        Env("alpha", "beta"),
        Add("http://example.com/big.tar.xz", "/usr/src/things/"),
        Copy("/tmp/foo", "/tmp/bar"),
        Entrypoint("myapp", Seq("--build", "/path/to/random/file")),
        Volume("myvolume"),
        WorkDir("/etc/bin/lib/dev/rand"),
        Expose(8080),
      ),
    )
    val validationErrors = validate(DockerfileGenerator.generate(content))
    validationErrors shouldBe empty
  }

  behavior of "DockerfileBuilderGenerator"
  it should "generate a valid dockerfile for a sample service" in {
    // We _have_ to include the service in a Templefile structure, so that the project can be correctly registered
    // and therefore allowing calls to lookupMetadata to function correctly.
    val templefile = Templefile(
      "ExampleProject",
      ProjectBlock(),
      services = Map("ComplexService" -> DockerGeneratorIntegrationTestData.sampleService),
    )

    templefile.services.foreach {
      case (name, service) =>
        val dockerfile =
          DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, 80, Some(Provider.DockerCompose))
        val generatedDockerfile = DockerfileGenerator.generate(dockerfile)
        val validationErrors    = validate(generatedDockerfile)
        validationErrors shouldBe empty
    }
  }
}
