package temple.generate.metrics.prometheus.ast

import io.circe.Json
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap

case class PrometheusConfig(jobs: Seq[PrometheusJob]) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "global" ~> ListMap(
      "scrape_interval"     ~> "15s",
      "evaluation_interval" ~> "15s",
    ),
    "scrape_configs" ~> jobs,
  )
}
