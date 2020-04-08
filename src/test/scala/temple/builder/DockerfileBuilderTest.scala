package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.AbstractServiceBlock.AuthServiceBlock
import temple.ast.Metadata.{Provider, ServiceLanguage}
import temple.ast.{ProjectBlock, Templefile}

class DockerfileBuilderTest extends FlatSpec with Matchers {

  behavior of "DockerfileBuilder"

  it should "generate a Dockerfile for simple Go project" in {
    // We _have_ to include the service in a Templefile structure, so that the project can be correctly registered
    // and therefore allowing calls to lookupMetadata to function correctly.
    val templefile = Templefile(
      "ExampleProject",
      ProjectBlock(Seq(ServiceLanguage.Go)),
      services = Map("SampleService" -> BuilderTestData.sampleService),
    )

    val dockerfile = templefile.services.head match {
      case (name, service) =>
        DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, 80, Some(Provider.Kubernetes))
    }

    dockerfile shouldBe DockerfileBuilderTestData.sampleServiceDockerfile
  }

  it should "generate a Dockerfile for complex Go project with kubernetes" in {
    val templefile = Templefile(
      "ExampleProject",
      ProjectBlock(Seq(ServiceLanguage.Go)),
      services = Map("ComplexService" -> BuilderTestData.sampleComplexService),
    )

    val dockerfile = templefile.services.head match {
      case (name, service) =>
        DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, 80, Some(Provider.Kubernetes))
    }

    dockerfile shouldBe DockerfileBuilderTestData.sampleComplexServiceDockerfile
  }

  it should "generate a Dockerfile for Go if no language is specified" in {
    val templefile = Templefile(
      "ExampleProject",
      services = Map("ComplexService" -> BuilderTestData.sampleComplexService),
    )

    val dockerfile = templefile.services.head match {
      case (name, service) =>
        DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, 80, Some(Provider.Kubernetes))
    }

    dockerfile shouldBe DockerfileBuilderTestData.sampleComplexServiceDockerfile
  }

  it should "generate the correct Dockerfile for a Go auth service when using DockerCompose" in {
    val dockerfile =
      DockerfileBuilder.createServiceDockerfile("auth", AuthServiceBlock, 80, Some(Provider.DockerCompose))
    dockerfile shouldBe DockerfileBuilderTestData.sampleAuthServiceDockerComposeDockerfile
  }

  it should "generate the correct Dockerfile for a Go auth service when using Kubernetes" in {
    val dockerfile = DockerfileBuilder.createServiceDockerfile("auth", AuthServiceBlock, 80, Some(Provider.Kubernetes))
    dockerfile shouldBe DockerfileBuilderTestData.sampleAuthServiceKubernetesDockerfile
  }
}
