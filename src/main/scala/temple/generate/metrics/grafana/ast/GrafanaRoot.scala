package temple.generate.metrics.grafana.ast

import io.circe.Json
import temple.generate.JsonEncodable

private[grafana] case class GrafanaRoot(uid: String, serviceName: String, panels: Seq[GrafanaPanel])
    extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "annotations"   ~> Map("list" -> Seq[String]()),
    "editable"      ~> true,
    "gnetId"        ~> None,
    "graphTooltip"  ~> 0,
    "id"            ~> None,
    "links"         ~> Seq[String](),
    "panels"        ~> panels,
    "refresh"       ~> "5s",
    "schemaVersion" ~> 22,
    "style"         ~> "dark",
    "tags"          ~> Seq[String](),
    "templating"    ~> Map("list" -> Seq[String]()),
    "time"          ~> Map("from" -> "now-15m", "to" -> "now"),
    "timepicker"    ~> Map("refresh_intervals" -> Seq("5s", "10s", "30s", "1m", "5m", "15m", "30m", "1h", "2h", "1d")),
    "timezone"      ~> "",
    "title"         ~> s"${serviceName} Dashboard",
    "uid"           ~> uid,
    "version"       ~> 1,
  )
}
