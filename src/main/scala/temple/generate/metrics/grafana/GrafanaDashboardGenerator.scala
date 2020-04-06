package temple.generate.metrics.grafana

import io.circe.syntax._
import temple.generate.metrics.grafana.ast.{GrafanaPanel, GrafanaRoot, GrafanaTarget, Row}

object GrafanaDashboardGenerator {
  private val maxWidth    = 24
  private val panelHeight = 5

  /**
    * Generate a JSON configuration file for Grafana
    * @param serviceName The name of the service
    * @param rows A sequence of rows, each of which contains a sequence of metrics to display
    * @return The raw JSON string
    */
  def generate(uid: String, serviceName: String, rows: Seq[Row]): String = {
    val grafanaPanels = rows.zipWithIndex.flatMap {
      case (row, index) =>
        val rowY       = index * panelHeight
        val panelWidth = maxWidth / row.metrics.size

        // Create a panel for each metric, with unique ID
        row.metrics.zipWithIndex.map {
          case (metric, metricIndex) =>
            // Define a target for each query with unique refID
            val targets = metric.queries.zip('A' to 'Z').map {
              case (query, refID) => GrafanaTarget(query.expression, query.legend, refID)
            }
            GrafanaPanel(
              metric.id,
              metric.title,
              metric.datasource,
              panelWidth,
              panelHeight,
              panelWidth * metricIndex,
              rowY,
              metric.yAxisLabel,
              targets,
            )
        }
    }

    GrafanaRoot(uid, serviceName, grafanaPanels).asJson.toString() + "\n"
  }
}
