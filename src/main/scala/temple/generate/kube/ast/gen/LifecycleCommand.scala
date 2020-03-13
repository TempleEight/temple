package temple.generate.kube.ast.gen

object LifecycleCommand {
  type LifecycleCommand = String
  val echoDone: LifecycleCommand  = "echo done"
  val psqlSetup: LifecycleCommand = "psql -U postgres -f /docker-entrypoint-initdb.d/init.sql && echo done"
}
