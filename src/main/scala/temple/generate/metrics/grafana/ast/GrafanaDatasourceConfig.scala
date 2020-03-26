package temple.generate.metrics.grafana.ast

import io.circe.Json
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap

private[grafana] case class GrafanaDatasourceConfig(datasource: Datasource) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    datasource match {
      case Datasource.Prometheus(name, url) =>
        Seq(
          "apiVersion" ~> 1,
          "datasources" ~> Seq(
            ListMap(
              "name"      ~> name,
              "type"      ~> datasource.typ,
              "access"    ~> "proxy",
              "orgId"     ~> 1,
              "url"       ~> url,
              "basicAuth" ~> false,
              "isDefault" ~> true,
              "editable"  ~> true,
            ),
          ),
        )
    }
}
