package temple.generate.metrics.grafana

import temple.utils.StringUtils

object GrafanaDashboardGeneratorTestUtils {

  private def makePanels(panels: Seq[String]): String =
    panels.map(panel => StringUtils.indent(panel, 4)).mkString(",")

  private def makeTargets(targets: Seq[String]): String =
    targets.map(target => StringUtils.indent(target, 4)).mkString(",")

  def makeTarget(expr: String, legend: String, refId: String): String =
    s"""
       |{
       |  "expr" : "$expr",
       |  "legendFormat" : "$legend",
       |  "refId" : "$refId"
       |}""".stripMargin

  def makePanel(
    id: Int,
    datasource: String,
    title: String,
    width: Int,
    height: Int,
    x: Int,
    y: Int,
    yAxisLabel: String,
    targets: String*,
  ): String = {
    val rawTargets = makeTargets(targets)
    s"""
       |{
       |  "aliasColors" : {
       |        
       |  },
       |  "bars" : false,
       |  "dashLength" : 10,
       |  "dashes" : false,
       |  "datasource" : "Prometheus",
       |  "fill" : 1,
       |  "fillGradient" : 0,
       |  "gridPos" : {
       |    "h" : $height,
       |    "w" : $width,
       |    "x" : $x,
       |    "y" : $y
       |  },
       |  "hiddenSeries" : false,
       |  "id" : $id,
       |  "legend" : {
       |    "show" : true,
       |    "avg" : false,
       |    "min" : false,
       |    "rightSide" : true,
       |    "total" : false,
       |    "max" : false,
       |    "values" : false,
       |    "current" : false
       |  },
       |  "lines" : true,
       |  "lineWidth" : 1,
       |  "nullPointMode" : "null",
       |  "options" : {
       |    "dataLinks" : [
       |    ]
       |  },
       |  "percentage" : false,
       |  "pointradius" : 2,
       |  "points" : false,
       |  "renderer" : "flot",
       |  "seriesOverrides" : [
       |  ],
       |  "spaceLength" : 10,
       |  "stack" : false,
       |  "steppedLine" : false,
       |  "targets" : [$rawTargets
       |  ],
       |  "thresholds" : [
       |  ],
       |  "timeFrom" : null,
       |  "timeRegions" : [
       |  ],
       |  "timeShift" : null,
       |  "title" : "$title",
       |  "tooltip" : {
       |    "shared" : true,
       |    "sort" : 0,
       |    "value_type" : "individual"
       |  },
       |  "type" : "graph",
       |  "x-axis" : {
       |    "name" : null,
       |    "show" : true,
       |    "buckets" : null,
       |    "mode" : "Time",
       |    "values" : [
       |    ]
       |  },
       |  "yaxes" : [
       |    {
       |      "format" : "short",
       |      "show" : true,
       |      "logBase" : 1,
       |      "label" : "$yAxisLabel",
       |      "min" : null,
       |      "max" : null
       |    },
       |    {
       |      "format" : "short",
       |      "show" : true,
       |      "logBase" : 1,
       |      "label" : null,
       |      "min" : null,
       |      "max" : null
       |    }
       |  ],
       |  "yaxis" : {
       |    "align" : false,
       |    "alignLevel" : null
       |  }
       |}""".stripMargin
  }

  def makeDashboard(uid: String, title: String, panels: String*): String = {
    val rawPanels = makePanels(panels)
    s"""|{
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
        |  "panels" : [$rawPanels
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
        |  "title" : "$title",
        |  "uid" : "$uid",
        |  "version" : 1
        |}""".stripMargin
  }
}
