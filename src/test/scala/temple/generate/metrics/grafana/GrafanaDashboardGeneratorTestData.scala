package temple.generate.metrics.grafana

object GrafanaDashboardGeneratorTestData {

  val emptyDashboard: String =
    """|{
       |  "annotations" : {
       |    "list" : [
       |    ]
       |  },
       |  "editable" : true,
       |  "gnetId" : null,
       |  "graphTooltip" : 0,
       |  "id" : null,
       |  "links" : [
       |  ],
       |  "panels" : [
       |  ],
       |  "refresh" : "5s",
       |  "schemaVersion" : 22,
       |  "style" : "dark",
       |  "tags" : [
       |  ],
       |  "templating" : {
       |    "list" : [
       |    ]
       |  },
       |  "time" : {
       |    "from" : "now-15m",
       |    "to" : "now"
       |  },
       |  "timepicker" : {
       |    "refresh_intervals" : [
       |      "5s",
       |      "10s",
       |      "30s",
       |      "1m",
       |      "5m",
       |      "15m",
       |      "30m",
       |      "1h",
       |      "2h",
       |      "1d"
       |    ]
       |  },
       |  "timezone" : "",
       |  "title" : "Example Dashboard",
       |  "uid" : "abcdefg",
       |  "version" : 1
       |}""".stripMargin
}
