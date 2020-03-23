package temple.generate.metrics.grafana

object GrafanaDashboardConfigGeneratorTestData {

  val prometheusConfig: String =
    """|apiVersion: 1
      |providers:
      |- name: Prometheus
      |  orgId: 1
      |  folder: ''
      |  type: file
      |  disableDeletion: false
      |  editable: true
      |  allowUiUpdates: true
      |  options:
      |    path: /etc/grafana/provisioning/dashboards
      |""".stripMargin
}
