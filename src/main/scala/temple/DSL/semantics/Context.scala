package temple.DSL.semantics

case class Context private (private val chain: List[String]) {
  def :+(string: String): Context = Context(string :: chain)

  override def toString: String = chain.mkString(", in ")

  def error(msg: String): SemanticParsingException = new SemanticParsingException(s"$msg in $this")
  def fail(msg: String): Nothing                   = throw error(msg)

  def apply[T](f: T => Context => Unit)(name: String, t: T): Unit = f(t)(this :+ name)
}

object Context {
  val empty: Context             = Context(List())
  def from(xs: String*): Context = xs.foldLeft(empty)(_ :+ _)
}
