package temple.generate.metrics.prometheus.ast

import io.circe.Json
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap

case class PrometheusJob(jobName: String, metricsUrl: String) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] = Seq(
    "job_name" ~> jobName,
    "static_configs" ~> Seq(
      ListMap(
        "targets" ~> Seq(metricsUrl),
      ),
    ),
  )
}
