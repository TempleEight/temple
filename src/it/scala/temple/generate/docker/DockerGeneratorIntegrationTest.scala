package temple.generate.docker

import org.scalatest.{BeforeAndAfter, Matchers}
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
    validate(DockerfileGenerator.generate(content)) shouldBe empty
  }
}
