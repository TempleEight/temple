package temple.generate.metrics.grafana

import io.circe.syntax._
import temple.generate.metrics.grafana.ast.GrafanaRoot
import temple.utils.StringUtils

object GrafanaDashboardGenerator {

  def generate(serviceName: String): String = generate(serviceName, StringUtils.randomString(8))

  private[grafana] def generate(serviceName: String, uid: String): String = {
    // TODO
    val panels = Seq()
    GrafanaRoot(serviceName, panels, uid).asJson.toString()
  }
}
