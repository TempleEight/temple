package temple.generate.orchestration.kube.ast

object RestartPolicy extends Enumeration {
  type RestartPolicy = Value
  val Always: RestartPolicy = Value("Always")
}
