package temple.generate.metrics.grafana.ast

import io.circe.Json
import temple.generate.JsonEncodable

private[grafana] case class GrafanaPanel(
  id: Int,
  title: String,
  datasource: String,
  width: Int,
  height: Int,
  x: Int,
  y: Int,
  yaxisLabel: String,
  targets: Seq[GrafanaTarget],
) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "aliasColors"  ~> Map[String, Json](),
    "bars"         ~> false,
    "dashLength"   ~> 10,
    "dashes"       ~> false,
    "datasource"   ~> datasource,
    "fill"         ~> 1,
    "fillGradient" ~> 0,
    "gridPos"      ~> Map("h" -> height, "w" -> width, "x" -> x, "y" -> y),
    "hiddenSeries" ~> false,
    "id"           ~> id,
    "legend" ~> Map(
      "avg"       -> false,
      "current"   -> false,
      "max"       -> false,
      "min"       -> false,
      "rightSide" -> true,
      "show"      -> true,
      "total"     -> false,
      "values"    -> false,
    ),
    "lines"           ~> true,
    "lineWidth"       ~> 1,
    "nullPointMode"   ~> "null",
    "options"         ~> Map("dataLinks" -> Seq[String]()),
    "percentage"      ~> false,
    "pointradius"     ~> 2,
    "points"          ~> false,
    "renderer"        ~> "flot",
    "seriesOverrides" ~> Seq[String](),
    "spaceLength"     ~> 10,
    "stack"           ~> false,
    "steppedLine"     ~> false,
    "targets"         ~> targets,
    "thresholds"      ~> Seq[String](),
    "timeFrom"        ~> None,
    "timeRegions"     ~> Seq[String](),
    "timeShift"       ~> None,
    "title"           ~> title,
    "tooltip"         ~> Map[String, Json]("shared" ~> true, "sort" ~> 0, "value_type" ~> "individual"),
    "type"            ~> "graph",
    "x-axis" ~> Map[String, Json](
      "buckets" ~> None,
      "mode"    ~> "Time",
      "name"    ~> None,
      "show"    ~> true,
      "values"  ~> Seq[String](),
    ),
    "yaxes" ~> Seq[Map[String, Json]](
      Map("format" ~> "short", "label" ~> yaxisLabel, "logBase" ~> 1, "max" ~> None, "min" ~> None, "show" ~> true),
      Map("format" ~> "short", "label" ~> None, "logBase"       ~> 1, "max" ~> None, "min" ~> None, "show" ~> true),
    ),
    "yaxis" ~> Map[String, Json]("align" ~> false, "alignLevel" ~> None),
  )
}
