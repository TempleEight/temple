package temple.DSL

import temple.utils.StringUtils.indent

/** Handles the internal representation of the DSL of the Templefile */
object Syntax {

  type Templefile = Seq[DSLRootItem]

  /** The type of a struct’s attribute, complete with parameters */
  sealed case class AttributeType(typeName: String, args: Args) {

    override def toString: String = {
      val argsStr = if (args.isEmpty) "" else s"($args)"
      typeName + argsStr
    }
  }

  /** The annotation of a struct’s attribute */
  sealed case class Annotation(key: String) { override def toString: String = s"@$key" }

  /** A value that can be passed as an argument to a type or metadata */
  sealed trait Arg

  object Arg {
    case class TokenArg(name: String)     extends Arg { override def toString: String = name                           }
    case class IntArg(value: Long)        extends Arg { override def toString: String = value.toString                 }
    case class FloatingArg(value: Double) extends Arg { override def toString: String = value.toString                 }
    case class ListArg(elems: Seq[Arg])   extends Arg { override def toString: String = elems.mkString("[", ", ", "]") }
    case object NoArg                     extends Arg { override def toString: String = "null"                         }
  }

  /** A sequence of values, both positional and named */
  case class Args(posargs: Seq[Arg] = Nil, kwargs: Seq[(String, Arg)] = Nil) {
    def size: Int        = posargs.size + kwargs.size
    def isEmpty: Boolean = posargs.isEmpty && kwargs.isEmpty

    override def toString: String = {
      val kwargsStr = kwargs.map { case (str, arg) => s"$str: $arg" }
      (posargs ++ kwargsStr).mkString(", ")
    }
  }

  /** Any element of a service/struct */
  sealed abstract class Entry(val typeName: String)

  object Entry {

    case class Attribute(key: String, dataType: AttributeType, annotations: Seq[Annotation] = Nil)
        extends Entry("attribute") {
      override def toString: String = s"$key: $dataType${annotations.map(" " + _).mkString};"
    }

    case class Metadata(metaKey: String, args: Args = Args()) extends Entry("metadata") {
      override def toString: String = s"#$metaKey ($args);"
    }
  }

  /** An item at the root of the Templefile, e.g. services and targets */
  sealed case class DSLRootItem(key: String, tag: String, entries: Seq[Entry]) extends Entry(s"$tag block ($key)") {

    override def toString: String = {
      val contents = indent(entries.mkString("\n"))
      s"$key: $tag {\n$contents\n}"
    }
  }
}
