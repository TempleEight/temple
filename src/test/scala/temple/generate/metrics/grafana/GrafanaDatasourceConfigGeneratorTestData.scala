package temple.generate.metrics.grafana

object GrafanaDatasourceConfigGeneratorTestData {

  val prometheusConfig: String =
    """apiVersion: 1
      |datasources:
      |- name: Prometheus
      |  type: prometheus
      |  access: proxy
      |  orgId: 1
      |  url: http://prom:9090
      |  basicAuth: false
      |  isDefault: true
      |  editable: true
      |""".stripMargin
}
