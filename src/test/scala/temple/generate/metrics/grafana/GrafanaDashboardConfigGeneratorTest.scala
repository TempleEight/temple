package temple.generate.metrics.grafana

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.metrics.grafana.GrafanaDashboardGeneratorTestUtils.{makeTarget, _}
import temple.generate.metrics.grafana.ast.Row
import temple.generate.metrics.grafana.ast.Row.{Metric, Query}

class GrafanaDashboardConfigGeneratorTest extends FlatSpec with Matchers {
  behavior of "GrafanaDashboardConfigGenerator"

  it should "generate correct config" in {
    val generated = GrafanaDashboardConfigGenerator.generate("Prometheus")
    generated shouldBe GrafanaDashboardConfigGeneratorTestData.prometheusConfig
  }
}
