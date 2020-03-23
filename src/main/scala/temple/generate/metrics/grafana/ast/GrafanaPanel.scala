package temple.generate.metrics.grafana.ast

import io.circe.Json
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap

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
    "aliasColors"  ~> ListMap[String, Json](),
    "bars"         ~> false,
    "dashLength"   ~> 10,
    "dashes"       ~> false,
    "datasource"   ~> datasource,
    "fill"         ~> 1,
    "fillGradient" ~> 0,
    "gridPos"      ~> ListMap("h" -> height, "w" -> width, "x" -> x, "y" -> y),
    "hiddenSeries" ~> false,
    "id"           ~> id,
    "legend" ~> ListMap(
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
    "linewidth"       ~> 1,
    "nullPointMode"   ~> "null",
    "options"         ~> ListMap("dataLinks" -> Seq[String]()),
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
    "tooltip"         ~> ListMap[String, Json]("shared" ~> true, "sort" ~> 0, "value_type" ~> "individual"),
    "type"            ~> "graph",
    "xaxis" ~> ListMap[String, Json](
      "buckets" ~> None,
      "mode"    ~> "time",
      "name"    ~> None,
      "show"    ~> true,
      "values"  ~> Seq[String](),
    ),
    "yaxes" ~> Seq[ListMap[String, Json]](
      ListMap("format" ~> "short", "label" ~> yaxisLabel, "logBase" ~> 1, "max" ~> None, "min" ~> None, "show" ~> true),
      ListMap("format" ~> "short", "label" ~> None, "logBase"       ~> 1, "max" ~> None, "min" ~> None, "show" ~> true),
    ),
    "yaxis" ~> ListMap[String, Json]("align" ~> false, "alignLevel" ~> None),
  )
}
