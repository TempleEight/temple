package temple.generate.server.config

import org.scalatest.{FlatSpec, Matchers}
import temple.ast.Metadata.Database
import temple.ast.Templefile.Ports

class ServerConfigGeneratorTest extends FlatSpec with Matchers {
  behavior of "ServerConfigGenerator"

  it should "generate correct config" in {
    val generated =
      ServerConfigGenerator.generate("match", Database.Postgres, Map("user" -> "http://user:80/user"), Ports(81, 2113))
    generated shouldBe ServerConfigGeneratorTestData.serverConfig
  }
}
