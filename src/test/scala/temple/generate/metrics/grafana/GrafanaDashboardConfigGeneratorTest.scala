package temple.generate.metrics.grafana

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.metrics.grafana.ast.Datasource

class GrafanaDashboardConfigGeneratorTest extends FlatSpec with Matchers {
  behavior of "GrafanaDashboardConfigGenerator"

  it should "generate correct config" in {
    val generated = GrafanaDashboardConfigGenerator.generate(Datasource.Prometheus("Prometheus"))
    generated shouldBe GrafanaDashboardConfigGeneratorTestData.prometheusConfig
  }
}
