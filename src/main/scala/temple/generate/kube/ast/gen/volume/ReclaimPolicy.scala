package temple.generate.kube.ast.gen.volume

object ReclaimPolicy extends Enumeration {
  type ReclaimPolicy = Value
  val Delete: ReclaimPolicy = Value("Delete")
}
