package temple.generate.metrics.grafana.ast

import io.circe.Json
import temple.generate.JsonEncodable

// TODO
case class Panel() extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    Seq()
}
