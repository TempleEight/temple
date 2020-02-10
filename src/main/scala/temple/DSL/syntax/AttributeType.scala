package temple.DSL.syntax

/** The type of a struct’s attribute, complete with parameters */
case class AttributeType(typeName: String, args: Args = Args()) {

  override def toString: String = {
    val argsStr = if (args.isEmpty) "" else s"($args)"
    typeName + argsStr
  }
}
