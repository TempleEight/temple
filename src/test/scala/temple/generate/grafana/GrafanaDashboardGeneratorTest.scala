package temple.generate.grafana

import org.scalatest.{FlatSpec, Matchers}

class GrafanaDashboardGeneratorTest extends FlatSpec with Matchers {
  behavior of "GrafanaDashboardGenerator"

  it should "generate a correct JSON file with no panels" in {
    val dashboard = GrafanaDashboardGenerator.generate("Example")
    dashboard shouldBe GrafanaDashboardGeneratorTestData.emptyDashboard
  }
}
