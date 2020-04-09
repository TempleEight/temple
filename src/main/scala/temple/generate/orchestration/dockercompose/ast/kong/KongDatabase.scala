package temple.generate.orchestration.dockercompose.ast.kong

import io.circe.Json
import temple.generate.orchestration.dockercompose.ast.Service

import scala.collection.immutable.ListMap

case object KongDatabase extends Service(Seq(), Seq("kong-network")) {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    ListMap(
      "image" ~> "postgres:12.1",
      "environment" ~> Seq(
        "POSTGRES_DB=kong",
        "POSTGRES_PASSWORD=kong",
        "POSTGRES_USER=kong",
      ),
      "healthcheck" ~> ListMap(
        "test"     ~> Seq("CMD", "pg_isready", "-U", "kong"),
        "interval" ~> "30s",
        "timeout"  ~> "30s",
        "retries"  ~> 3,
      ),
      "restart"    ~> "on-failure",
      "stdin_open" ~> true,
      "tty"        ~> true,
    ) ++ super.jsonEntryIterator
}
