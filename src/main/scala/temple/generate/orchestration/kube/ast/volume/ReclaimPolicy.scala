package temple.generate.orchestration.kube.ast.volume

object ReclaimPolicy extends Enumeration {
  type ReclaimPolicy = Value
  val Delete: ReclaimPolicy = Value("Delete")
}
