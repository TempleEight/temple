package temple.generate.kube

/** Case class to encapsulate which Kubernetes object kind being generated */
sealed trait GenType

private[kube] object GenType {
  case object Service      extends GenType
  case object Deployment   extends GenType
  case object StorageClaim extends GenType
  case object StorageMount extends GenType
}