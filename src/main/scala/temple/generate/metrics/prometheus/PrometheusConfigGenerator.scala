package temple.generate.metrics.prometheus

import io.circe.yaml.Printer
import io.circe.syntax._
import io.circe.yaml.Printer.FlowStyle
import temple.generate.metrics.prometheus.ast.{PrometheusConfig, PrometheusJob}

object PrometheusConfigGenerator {

  def generate(jobs: Seq[PrometheusJob]): String =
    Printer(preserveOrder = true).pretty(PrometheusConfig(jobs).asJson)
}
