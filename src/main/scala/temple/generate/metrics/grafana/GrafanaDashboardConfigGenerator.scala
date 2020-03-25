package temple.generate.metrics.grafana

import io.circe.yaml.Printer
import io.circe.syntax._
import temple.generate.metrics.grafana.ast.GrafanaDashboardConfig

object GrafanaDashboardConfigGenerator {

  def generate(providerName: String): String =
    Printer(preserveOrder = true).pretty(GrafanaDashboardConfig(providerName).asJson)
}
