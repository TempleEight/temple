package temple.generate.metrics.grafana.ast

import io.circe.Json
import temple.generate.JsonEncodable

private[grafana] case class GrafanaTarget(query: String, legendFormat: String, refID: Char)
    extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "expr"         ~> query,
    "legendFormat" ~> legendFormat,
    "refId"        ~> refID,
  )
}
