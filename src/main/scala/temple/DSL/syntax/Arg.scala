package temple.DSL.Syntax

/** A value that can be passed as an argument to a type or metadata */
sealed trait Arg

object Arg {
  case class TokenArg(name: String)     extends Arg { override def toString: String = name                           }
  case class IntArg(value: scala.Int)   extends Arg { override def toString: String = value.toString                 }
  case class FloatingArg(value: Double) extends Arg { override def toString: String = value.toString                 }
  case class ListArg(elems: Seq[Arg])   extends Arg { override def toString: String = elems.mkString("[", ", ", "]") }
}
