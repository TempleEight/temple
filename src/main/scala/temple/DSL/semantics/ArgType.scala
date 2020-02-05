package temple.DSL.semantics

import temple.DSL.Syntax
import temple.DSL.Syntax.Arg
import temple.utils.SeqUtils._

/** The type of an argument passed to any function-like item in the DSL, along with a method of extracting such a value
  * from an [[temple.DSL.Syntax.Arg]] */
sealed abstract class ArgType[T](val stringRep: String) {

  /**
    * Extract [[Some]] value of given [[temple.DSL.semantics.ArgType]] from a [[temple.DSL.Syntax.Arg]], or [[None]] if
    * a different type of value is present
    *
    * @param arg the argument of unknown type from which to try to extract a value of this type
    * @return [[Some]] value of this type, or [[None]] if it is of a different type
    */
  def extractArg(arg: Syntax.Arg): Option[T]
}

object ArgType {

  case object IntArgType extends ArgType[Long]("integer") {

    override def extractArg(arg: Arg): Option[Long] =
      arg match { case Arg.IntArg(value) => Some(value); case _ => None }
  }

  case object FloatingArgType extends ArgType[Double]("floating point") {

    override def extractArg(arg: Arg): Option[Double] =
      arg match { case Arg.IntArg(value) => Some(value); case Arg.FloatingArg(value) => Some(value); case _ => None }
  }

  case object TokenArgType extends ArgType[String]("token") {

    override def extractArg(arg: Arg): Option[String] =
      arg match { case Arg.TokenArg(value) => Some(value); case _ => None }
  }

  case object StringArgType extends ArgType[String]("string") {

    override def extractArg(arg: Arg): Option[String] =
      arg match { case Arg.TokenArg(value) => Some(value); case _ => None }
  }

  case class ListArgType[T](elemType: ArgType[T]) extends ArgType[Seq[T]](s"${elemType.stringRep} list") {

    override def extractArg(arg: Arg): Option[Seq[T]] =
      arg match { case Arg.ListArg(elems) => elems.map(elemType.extractArg).sequence; case _ => None }
  }
}
