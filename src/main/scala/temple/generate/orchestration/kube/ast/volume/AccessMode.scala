package temple.generate.orchestration.kube.ast.volume

object AccessMode extends Enumeration {
  type AccessMode = Value
  val ReadWriteMany: AccessMode = Value("ReadWriteMany")
}
