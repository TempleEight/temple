package temple.builder

import org.scalatest.{FlatSpec, Matchers}
import temple.DSL.semantics.Metadata.ServiceLanguage
import temple.DSL.semantics.{ProjectBlock, Templefile}

class DockerBuilderTest extends FlatSpec with Matchers {
  behavior of "DockerBuilder"

  it should "generate a Dockerfile for simple Go project" in {
    // We _have_ to include the service in a Templefile structure, so that the project can be correctly registered
    // and therefore allowing calls to lookupMetadata to function correctly.
    val templefile = Templefile(
      "ExampleProject",
      ProjectBlock(Seq(ServiceLanguage.Go)),
      targets = Map(),
      services = Map("SampleService" -> BuilderTestData.sampleService),
    )

    val dockerfile = templefile.services.head match {
      case (name, service) => DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, 80)
    }

    dockerfile shouldBe DockerBuilderTestData.sampleServiceDockerfile
  }

  it should "generate a Dockerfile for complex Go project" in {
    val templefile = Templefile(
      "ExampleProject",
      ProjectBlock(Seq(ServiceLanguage.Go)),
      targets = Map(),
      services = Map("ComplexService" -> BuilderTestData.sampleComplexService),
    )

    val dockerfile = templefile.services.head match {
      case (name, service) => DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, 80)
    }

    dockerfile shouldBe DockerBuilderTestData.sampleComplexServiceDockerfile
  }

  it should "generate a Dockerfile for Go if no language is specified" in {
    val templefile = Templefile(
      "ExampleProject",
      ProjectBlock(),
      targets = Map(),
      services = Map("ComplexService" -> BuilderTestData.sampleComplexService),
    )

    val dockerfile = templefile.services.head match {
      case (name, service) => DockerfileBuilder.createServiceDockerfile(name.toLowerCase, service, 80)
    }

    dockerfile shouldBe DockerBuilderTestData.sampleComplexServiceDockerfile
  }
}
