package temple.generate.server.config

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.Metadata.Database
import temple.ast.Metadata.Metrics.Prometheus
import temple.ast.Templefile.Ports

class ServerConfigGeneratorTest extends FlatSpec with Matchers {
  behavior of "ServerConfigGenerator"

  it should "generate correct config without metrics" in {
    val generated =
      ServerConfigGenerator
        .generate("match", Database.Postgres, Map("user" -> "http://user:80/user"), Ports(81, 2113), None)
    generated shouldBe ServerConfigGeneratorTestData.serverConfig
  }

  it should "generate correct config with metrics" in {
    val generated =
      ServerConfigGenerator
        .generate("match", Database.Postgres, Map("user" -> "http://user:80/user"), Ports(81, 2113), Some(Prometheus))
    generated shouldBe ServerConfigGeneratorTestData.serverConfigWithMetrics
  }
}
