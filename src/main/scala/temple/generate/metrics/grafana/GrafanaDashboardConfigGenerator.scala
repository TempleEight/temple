package temple.generate.metrics.grafana

import io.circe.syntax._
import io.circe.yaml.Printer
import temple.generate.metrics.grafana.ast.{Datasource, GrafanaDashboardConfig}

object GrafanaDashboardConfigGenerator {

  def generate(datasource: Datasource): String =
    Printer(preserveOrder = true).pretty(GrafanaDashboardConfig(datasource).asJson)
}
