package temple.generate.orchestration.kube.ast

object LifecycleCommand extends Enumeration {
  type LifecycleCommand = Value
  val echoDone: LifecycleCommand  = Value("echo done")
  val psqlSetup: LifecycleCommand = Value("psql -U postgres -f /docker-entrypoint-initdb.d/init.sql && echo done")
}
