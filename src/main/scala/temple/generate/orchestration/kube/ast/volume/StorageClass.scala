package temple.generate.orchestration.kube.ast.volume

object StorageClass extends Enumeration {
  type StorageClass = Value
  val Manual: StorageClass = Value("manual")
}
