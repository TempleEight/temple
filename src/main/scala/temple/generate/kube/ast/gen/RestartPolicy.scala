package temple.generate.kube.ast.gen

object RestartPolicy extends Enumeration {
  type RestartPolicy = Value
  val Always: RestartPolicy = Value("Always")
}
