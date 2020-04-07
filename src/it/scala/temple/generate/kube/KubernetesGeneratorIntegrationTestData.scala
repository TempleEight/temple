package temple.generate.kube

import temple.generate.kube.ast.OrchestrationType.{DbStorage, OrchestrationRoot, Service}
import temple.generate.kube.ast.gen.LifecycleCommand

object KubernetesGeneratorIntegrationTestData {

  val basicOrchestrationRoot: OrchestrationRoot = OrchestrationRoot(
    Seq(
      Service(
        name = "user",
        image = "temple-user-service",
        dbImage = "postgres:12.1",
        ports = Seq("api" -> 80),
        replicas = 1,
        secretName = "regcred",
        appEnvVars = Seq(),
        dbEnvVars = Seq("PGUSER" -> "postgres"),
        dbStorage = DbStorage(
          dataMount = "/var/lib/postgresql/data",
          initMount = "/docker-entrypoint-initdb.d/init.sql",
          initFile = "init.sql",
          hostPath = "/data/user-db",
        ),
        dbLifecycleCommand = LifecycleCommand.echoDone.toString,
        usesAuth = true,
      ),
    ),
    usesMetrics = true,
  )
}
