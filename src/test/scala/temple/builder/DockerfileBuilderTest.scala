package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.AbstractAttribute.Attribute
import temple.ast.AbstractServiceBlock.{AuthServiceBlock, ServiceBlock}
import temple.ast.Metadata.{Provider, ServiceLanguage}
import temple.ast.{AttributeType, Metadata, ProjectBlock, Templefile}

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
    val templefile = Templefile(
      "ExampleProject",
      services = Map(
        "AuthyService" -> ServiceBlock(
          attributes = Map("test" -> Attribute(AttributeType.StringType())),
          metadata = Seq(Metadata.ServiceAuth.Email),
          structs = Map(),
        ),
      ),
    )

    val authBlock = templefile.allServices
      .collectFirst {
        case (_, AuthServiceBlock) => AuthServiceBlock
      }
      .getOrElse(throw new RuntimeException("Auth block doesn't exist"))

    val dockerfile =
      DockerfileBuilder.createServiceDockerfile("auth", authBlock, 80, Some(Provider.DockerCompose))
    dockerfile shouldBe DockerfileBuilderTestData.sampleAuthServiceDockerComposeDockerfile
  }

  it should "generate the correct Dockerfile for a Go auth service when using Kubernetes" in {
    val templefile = Templefile(
      "ExampleProject",
      services = Map(
        "AuthyService" -> ServiceBlock(
          attributes = Map("test" -> Attribute(AttributeType.StringType())),
          metadata = Seq(Metadata.ServiceAuth.Email),
          structs = Map(),
        ),
      ),
    )

    val authBlock = templefile.allServices
      .collectFirst {
        case (_, AuthServiceBlock) => AuthServiceBlock
      }
      .getOrElse(throw new RuntimeException("Auth block doesn't exist"))

    val dockerfile = DockerfileBuilder.createServiceDockerfile("auth", authBlock, 80, Some(Provider.Kubernetes))
    dockerfile shouldBe DockerfileBuilderTestData.sampleAuthServiceKubernetesDockerfile
  }
}
