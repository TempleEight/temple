package temple.generate.metrics.grafana.ast

import io.circe.Json
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap

private[grafana] case class GrafanaDashboardConfig(provider: String) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    Seq(
      "apiVersion" ~> 1,
      "providers" ~> Seq(
        ListMap(
          "name"            ~> provider,
          "orgId"           ~> 1,
          "folder"          ~> "",
          "type"            ~> "file",
          "disableDeletion" ~> false,
          "editable"        ~> true,
          "allowUiUpdates"  ~> true,
          "options"         ~> ListMap("path" ~> "/etc/grafana/provisioning/dashboards"),
        ),
      ),
    )
}
