package temple.generate.orchestration.dockercompose.ast.kong

import io.circe.Json
import temple.generate.orchestration.dockercompose.ast.Service

import scala.collection.immutable.ListMap

case object KongService extends Service(Seq(8000, 8001, 8443, 8444), Seq("kong-network")) {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    ListMap(
      "image"      ~> "kong:2.0.2",
      "user"       ~> "kong",
      "depends_on" ~> Seq("kong-db"),
      "environment" ~> Seq(
        "KONG_ADMIN_ACCESS_LOG=/dev/stdout",
        "KONG_ADMIN_ERROR_LOG=/dev/stderr",
        "KONG_PROXY_ACCESS_LOG=/dev/stdout",
        "KONG_PROXY_ERROR_LOG=/dev/stderr",
        "KONG_ADMIN_LISTEN=0.0.0.0:8001",
        "KONG_CASSANDRA_CONTACT_POINTS=kong-db",
        "KONG_DATABASE=postgres",
        "KONG_PG_DATABASE=kong",
        "KONG_PG_HOST=kong-db",
        "KONG_PG_PASSWORD=kong",
        "KONG_PG_USER=kong",
      ),
      "healthcheck" ~> ListMap(
        "test"     ~> Seq("CMD", "kong", "health"),
        "interval" ~> "10s",
        "timeout"  ~> "10s",
        "retries"  ~> 10,
      ),
      "restart" ~> "on-failure",
    ) ++ super.jsonEntryIterator
}
