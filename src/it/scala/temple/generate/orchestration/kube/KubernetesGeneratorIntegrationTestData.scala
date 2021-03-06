package temple.generate.orchestration.kube

import temple.ast.Templefile.Ports
import temple.generate.orchestration.ast.OrchestrationType.{DbStorage, OrchestrationRoot, Service}
import temple.generate.orchestration.kube.ast.LifecycleCommand

object KubernetesGeneratorIntegrationTestData {

  val basicOrchestrationRoot: OrchestrationRoot = OrchestrationRoot(
    Seq(
      Service(
        name = "user",
        image = "temple-user-service",
        dbImage = "postgres:12.1",
        ports = Ports(80, 81),
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
