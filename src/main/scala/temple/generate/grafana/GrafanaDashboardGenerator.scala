package temple.generate.grafana

import io.circe.syntax._
import temple.generate.grafana.ast.GrafanaRoot

object GrafanaDashboardGenerator {

  def generate(serviceName: String): String = {
    // TODO
    val panels = Seq()
    GrafanaRoot(serviceName, panels).asJson.toString()
  }
}
