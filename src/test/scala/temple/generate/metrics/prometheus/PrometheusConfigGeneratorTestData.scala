package temple.generate.metrics.prometheus

object PrometheusConfigGeneratorTestData {

  val prometheusConfig: String =
    """global:
      |  scrape_interval: 15s
      |  evaluation_interval: 15s
      |scrape_configs:
      |- job_name: user
      |  static_configs:
      |  - targets:
      |    - user:2112
      |- job_name: match
      |  static_configs:
      |  - targets:
      |    - match:2113
      |- job_name: auth
      |  static_configs:
      |  - targets:
      |    - auth:2114
      |""".stripMargin
}
