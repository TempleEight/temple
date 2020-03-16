package temple.generate.kube.ast.gen.volume

object StorageClass extends Enumeration {
  type StorageClass = Value
  val Manual: StorageClass = Value("manual")
}
