package temple.DSL.syntax

/** A sequence of values, both positional and named */
case class Args(posargs: Seq[Arg] = Nil, kwargs: Seq[(String, Arg)] = Nil) {
  def isEmpty: Boolean = posargs.isEmpty && kwargs.isEmpty

  override def toString: String = {
    val kwargsStr = kwargs.map { case (str, arg) => s"$str: $arg" }
    (posargs ++ kwargsStr).mkString(", ")
  }
}
