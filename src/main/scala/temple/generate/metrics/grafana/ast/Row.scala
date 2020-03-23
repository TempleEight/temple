package temple.generate.metrics.grafana.ast

import temple.generate.metrics.grafana.ast.Row.Metric

/** A row expresses a horizontal sequence of metric graphs placed on the dashboard */
case class Row(metrics: Metric*)

object Row {

  /** A metric defines a single dashboard item, potentially showing multiple queries */
  case class Metric(id: Int, title: String, datasource: String, yAxisLabel: String, queries: Seq[Query])

  /** A query is a single expression, along with a formatted legend
    * See https://grafana.com/docs/grafana/latest/features/datasources/prometheus/#query-editor */
  case class Query(expression: String, legend: String)
}
