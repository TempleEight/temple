package temple.generate.metrics.grafana

import org.scalatest.{FlatSpec, Matchers}

class GrafanaDashboardGeneratorTest extends FlatSpec with Matchers {
  behavior of "GrafanaDashboardGenerator"

  it should "generate a correct JSON file with no panels" in {
    val dashboard = GrafanaDashboardGenerator.generate("Example", "abcdefg")
    dashboard shouldBe GrafanaDashboardGeneratorTestData.emptyDashboard
  }
}
