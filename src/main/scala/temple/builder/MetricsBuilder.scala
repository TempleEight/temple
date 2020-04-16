package temple.builder

import temple.generate.CRUD.CRUD
import temple.generate.metrics.grafana.ast.Row.{Metric, Query}
import temple.generate.metrics.grafana.ast.{Datasource, Row}

import scala.collection.immutable.SortedSet

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

  def createDashboardRows(
    serviceName: String,
    datasource: Datasource,
    endpoints: SortedSet[CRUD],
    structName: Option[String] = None,
  ): Seq[Row] =
    endpoints.zipWithIndex.map {
      case (endpoint, index) =>
        Row(
          Metric(
            index * 2,
            s"$endpoint ${structName.getOrElse(serviceName)} Requests",
            datasource,
            "QPS",
            qpsQueries(
              serviceName.toLowerCase,
              (endpoint.toString + structName.fold("")(name => s"_$name")).toLowerCase,
            ),
          ),
          Metric(
            index * 2 + 1,
            s"DB $endpoint ${structName.fold("Queries")(name => s"$name Queries")}",
            datasource,
            "Time (seconds)",
            databaseDurationQueries(
              serviceName.toLowerCase,
              (endpoint.toString + structName.fold("")(name => s"_$name")).toLowerCase,
            ),
          ),
        )
    }.toSeq
}
