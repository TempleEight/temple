package temple.DSL.syntax

/** A value that can be passed as an argument to a type or metadata */
sealed trait Arg {
  def toTempleString: String
}

object Arg {
  case class TokenArg(name: String)     extends Arg { override def toTempleString: String = name           }
  case class IntArg(value: scala.Int)   extends Arg { override def toTempleString: String = value.toString }
  case class FloatingArg(value: Double) extends Arg { override def toTempleString: String = value.toString }

  case class ListArg(elems: Seq[Arg]) extends Arg {
    override def toTempleString: String = elems.map(_.toTempleString).mkString("[", ", ", "]")
  }

  case object NoArg extends Arg {

    override def toTempleString: String =
      throw new UnsupportedOperationException("NoArg cannot be represented in a Templefile")
  }
}
