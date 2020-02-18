package temple.DSL.syntax

sealed trait AttributeType {
  val typeName: String
}

object AttributeType {

  /** The type of a struct’s attribute, complete with parameters */
  case class Primitive(typeName: String, args: Args = Args()) extends AttributeType {

    override def toString: String = {
      val argsStr = if (args.isEmpty) "" else s"($args)"
      typeName + argsStr
    }
  }

  case class Foreign(typeName: String) extends AttributeType {
    override def toString: String = typeName
  }
}
