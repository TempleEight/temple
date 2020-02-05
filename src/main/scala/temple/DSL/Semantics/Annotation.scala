package temple.DSL.Semantics

abstract class Annotation(val render: String)

object Annotation {
  abstract class AccessAnnotation(render: String) extends Annotation(render)
  abstract class ValueAnnotation(render: String)  extends Annotation(render)

  case object Unique    extends ValueAnnotation("@unique")
  case object Server    extends AccessAnnotation("@server")
  case object ServerSet extends AccessAnnotation("@serverSet")
  case object Client    extends AccessAnnotation("@client")
}
