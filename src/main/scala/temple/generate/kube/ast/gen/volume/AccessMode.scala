package temple.generate.kube.ast.gen.volume

object AccessMode extends Enumeration {
  type AccessMode = Value
  val ReadWriteMany: AccessMode = Value("ReadWriteMany")
}
