package temple.generate.kube.ast.gen

object PlacementStrategy extends Enumeration {
  type Strategy = Value
  val Recreate: Strategy = Value("Recreate")
}
