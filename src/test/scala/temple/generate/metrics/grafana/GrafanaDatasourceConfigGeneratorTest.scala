package temple.generate.metrics.grafana

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.metrics.grafana.ast.Datasource

class GrafanaDatasourceConfigGeneratorTest extends FlatSpec with Matchers {
  behavior of "GrafanaDatasourceConfigGenerator"

  it should "generate correct config" in {
    val generated = GrafanaDatasourceConfigGenerator.generate(Datasource.Prometheus("Prometheus", "http://prom:9090"))
    generated shouldBe GrafanaDatasourceConfigGeneratorTestData.prometheusConfig
  }
}
