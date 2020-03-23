package temple.builder

import temple.generate.CRUD
import temple.generate.metrics.grafana.ast.Row
import temple.generate.metrics.grafana.ast.Row.{Metric, Query}

object MetricsBuilder {

  // Generate PromQL queries for determining queries per second to a service
  private def qpsQueries(serviceName: String, queryType: String): Seq[Query] =
    Seq(
      Query(
        s"""rate(${serviceName}_request_success_total{request_type=\"$queryType\"}[5m])""",
        "Success",
      ),
      Query(
        s"""rate(${serviceName}_request_failure_total{request_type=\"$queryType\"}[5m])""",
        "{{error_code}}",
      ),
    )

  // Generate PromQL queries for determining duration of database requests
  private def databaseDurationQueries(serviceName: String, queryType: String): Seq[Query] =
    Seq(
      Query(
        s"""${serviceName}_database_request_seconds{quantile=\"0.5\",query_type=\"$queryType\"}""",
        "50th Percentile",
      ),
      Query(
        s"""${serviceName}_database_request_seconds{quantile=\"0.9\",query_type=\"$queryType\"}""",
        "90th Percentile",
      ),
      Query(
        s"""${serviceName}_database_request_seconds{quantile=\"0.95\",query_type=\"$queryType\"}""",
        "95th Percentile",
      ),
      Query(
        s"""${serviceName}_database_request_seconds{quantile=\"0.99\",query_type=\"$queryType\"}""",
        "99th Percentile",
      ),
    )

  def createDashboardRows(serviceName: String, endpoints: Set[CRUD]): Seq[Row] =
    endpoints.zipWithIndex.map {
      case (endpoint, index) =>
        Row(
          Metric(
            index * 2,
            s"$endpoint $serviceName Requests",
            "Prometheus",
            "QPS",
            qpsQueries(serviceName.toLowerCase, endpoint.toString.toLowerCase),
          ),
          Metric(
            index * 2 + 1,
            s"DB $endpoint Queries",
            "Prometheus",
            "Time (seconds)",
            databaseDurationQueries(serviceName.toLowerCase, endpoint.toString.toLowerCase),
          ),
        )
    }.toSeq
}
