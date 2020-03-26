package temple.generate.metrics.prometheus

import org.scalatest.{FlatSpec, Matchers}
import temple.generate.metrics.prometheus.ast.PrometheusJob

class PrometheusConfigGeneratorTest extends FlatSpec with Matchers {
  behavior of "PrometheusConfigGenerator"

  it should "generate correct config" in {
    val generated = PrometheusConfigGenerator.generate(
      Seq(
        PrometheusJob("user", "user:2112"),
        PrometheusJob("match", "match:2113"),
        PrometheusJob("auth", "auth:2114"),
      ),
    )
    generated shouldBe PrometheusConfigGeneratorTestData.prometheusConfig
  }
}
