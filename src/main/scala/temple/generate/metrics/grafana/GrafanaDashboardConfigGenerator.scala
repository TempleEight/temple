package temple.generate.metrics.grafana

import io.circe.yaml.Printer
import io.circe.syntax._
import temple.generate.metrics.grafana.ast.{GrafanaDashboardConfig, Datasource}

object GrafanaDashboardConfigGenerator {

  def generate(datasource: Datasource): String =
    Printer(preserveOrder = true).pretty(GrafanaDashboardConfig(datasource).asJson)
}
