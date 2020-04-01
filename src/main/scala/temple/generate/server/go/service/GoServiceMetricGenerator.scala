package temple.generate.server.go.service

import temple.generate.CRUD.CRUD
import temple.generate.server.ServiceRoot
import temple.generate.server.go.common.GoCommonGenerator
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}
import temple.utils.StringUtils.doubleQuote
import temple.generate.server.go.common.GoCommonGenerator.genAssign
import temple.generate.utils.CodeUtils

import scala.collection.immutable.ListMap

object GoServiceMetricGenerator {

  private[service] def generateImports(): String =
    mkCode(
      "import",
      CodeWrap.parens.tabbed(
        Seq(
          "github.com/prometheus/client_golang/prometheus",
          "github.com/prometheus/client_golang/prometheus/promauto",
        ).map(doubleQuote),
      ),
    )

  private def generatePrometheusCounter(name: String, help: String, tags: Seq[String]): String =
    GoCommonGenerator.genFunctionCall(
      "promauto.NewCounterVec",
      GoCommonGenerator.genPopulateStruct(
        "prometheus.CounterOpts",
        ListMap(
          "Name" -> doubleQuote(name),
          "Help" -> doubleQuote(help),
        ),
      ),
      CodeWrap.curly.prefix("[]string").list(tags.map(doubleQuote)),
    )

  private def generatePrometheusSummary(
    name: String,
    help: String,
    objectives: Seq[(Double, Double)],
    tags: Seq[String],
  ): String =
    GoCommonGenerator.genFunctionCall(
      "promauto.NewSummaryVec",
      GoCommonGenerator.genPopulateStruct(
        "prometheus.SummaryOpts",
        ListMap(
          "Name" -> doubleQuote(name),
          "Help" -> doubleQuote(help),
          "Objectives" -> CodeWrap.curly
            .prefix("map[float64]float64")
            .list(objectives.map {
              case (k, v) =>
                s"$k: $v"
            }),
        ),
      ),
      CodeWrap.curly.prefix("[]string").list(tags.map(doubleQuote)),
    )

  // Generate global variables for metrics, including string identifiers and metric objects
  private[service] def generateVars(root: ServiceRoot, operations: Set[CRUD]): String = {
    // Assign strings to variables of form `RequestCreate = "create"`
    val serviceStrings = CodeUtils.pad(operations.toSeq.sorted.map { operation =>
      (s"Request${operation.toString.capitalize}", doubleQuote(operation.toString.toLowerCase))
    }, separator = " = ")

    val successCounter = genAssign(
      generatePrometheusCounter(
        name = s"${root.name.toLowerCase}_request_success_total",
        help = "The total number of successful requests",
        tags = Seq("request_type"),
      ),
      "RequestSuccess",
    )

    val failureCounter = genAssign(
      generatePrometheusCounter(
        name = s"${root.name.toLowerCase}_request_failure_total",
        help = "The total number of failed requests",
        tags = Seq("request_type", "error_code"),
      ),
      "RequestFailure",
    )

    val databaseSummary = genAssign(
      generatePrometheusSummary(
        name = s"${root.name.toLowerCase}_database_request_seconds",
        help = "The time spent executing database requests in seconds",
        objectives = Seq(0.5 -> 0.05, 0.9 -> 0.01, 0.95 -> 0.005, 0.99 -> 0.001),
        tags = Seq("query_type"),
      ),
      "DatabaseRequestDuration",
    )

    // Wrap all assignments in a `var` block
    mkCode("var", CodeWrap.parens.tabbed(serviceStrings, "", successCounter, "", failureCounter, "", databaseSummary))
  }
}
