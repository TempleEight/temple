package temple.generate.orchestration.dockercompose.ast.kong

import io.circe.Json
import temple.generate.orchestration.dockercompose.ast.Service

import scala.collection.immutable.ListMap

case object KongMigrations extends Service(Seq(), Seq("kong-network")) {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    ListMap(
      "image"      ~> "kong:2.0.1",
      "command"    ~> "kong migrations bootstrap && kong migrations up && kong migrations finish",
      "depends_on" ~> Seq("kong-db"),
      "environment" ~> Seq(
        "KONG_DATABASE=postgres",
        "KONG_PG_DATABASE=kong",
        "KONG_PG_HOST=kong-db",
        "KONG_PG_PASSWORD=kong",
        "KONG_PG_USER=kong",
      ),
      "restart" ~> "on-failure",
    ) ++ super.jsonEntryIterator
}
