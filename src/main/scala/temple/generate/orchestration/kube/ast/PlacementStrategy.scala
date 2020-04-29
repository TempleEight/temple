package temple.generate.orchestration.kube.ast

object PlacementStrategy extends Enumeration {
  type Strategy = Value
  val Recreate: Strategy = Value("Recreate")
}
