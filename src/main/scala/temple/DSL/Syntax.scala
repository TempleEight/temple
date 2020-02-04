package temple.DSL

import temple.utils.StringUtils.indent

/** Handles the internal representation of the DSL of the Templefile */
object Syntax {

  type Templefile = List[DSLRootItem]

  /** The type of a struct’s attribute, complete with parameters */
  sealed case class AttributeType(typeName: String, args: Args) {

    override def toString: String = {
      val argsStr = if (args.isEmpty) "" else s"($args)"
      typeName + argsStr
    }
  }

  /** The annotation of a struct’s attribute */
  sealed case class Annotation(key: String)

  /** A value that can be passed as an argument to a type or metadata */
  sealed trait Arg

  object Arg {
    case class TokenArg(name: String)     extends Arg { override def toString: String = name                           }
    case class IntArg(value: Long)        extends Arg { override def toString: String = value.toString                 }
    case class FloatingArg(value: Double) extends Arg { override def toString: String = value.toString                 }
    case class ListArg(elems: List[Arg])  extends Arg { override def toString: String = elems.mkString("[", ", ", "]") }
    case object NoArg                     extends Arg { override def toString: String = "null"                         }
  }

  /** A sequence of values, both positional and named */
  case class Args(posargs: List[Arg] = List(), kwargs: List[(String, Arg)] = List()) {
    def size: Int        = posargs.size + kwargs.size
    def isEmpty: Boolean = posargs.isEmpty && kwargs.isEmpty

    override def toString: String = {
      val kwargsStr = kwargs.map { case (str, arg) => s"$str: $arg" }
      (posargs ++ kwargsStr).mkString(", ")
    }
  }

  /** Any element of a service/struct */
  sealed trait Entry

  object Entry {

    case class Attribute(key: String, dataType: AttributeType, annotations: List[Annotation]) extends Entry {
      override def toString: String = s"$key: $dataType${annotations.map(" " + _).mkString};"
    }

    case class Metadata(metaKey: String, args: Args) extends Entry {
      override def toString: String = s"#$metaKey ($args);"
    }
  }

  /** An item at the root of the Templefile, e.g. services and targets */
  sealed case class DSLRootItem(key: String, tag: String, entries: List[Entry]) extends Entry {

    override def toString: String = {
      val contents = indent(entries.mkString("\n"))
      s"$key: $tag {\n$contents\n}"
    }
  }
}
