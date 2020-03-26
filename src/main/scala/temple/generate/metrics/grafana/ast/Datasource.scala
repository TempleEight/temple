package temple.generate.metrics.grafana.ast

sealed abstract class Datasource(val name: String, val typ: String)

object Datasource {
  // https://grafana.com/docs/grafana/v6.6/features/datasources/prometheus/
  case class Prometheus(override val name: String) extends Datasource(name, "prometheus")
}
