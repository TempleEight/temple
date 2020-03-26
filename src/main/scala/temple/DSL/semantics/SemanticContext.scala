package temple.DSL.semantics

import temple.errors.ErrorHandlingContext

final case class SemanticContext private (private val chain: List[String]) extends ErrorHandlingContext {
  def :+(string: String): SemanticContext = SemanticContext(string :: chain)

  def apply[T](f: T => SemanticContext => Unit)(name: String, t: T): Unit = f(t)(this :+ name)

  override def toString: String = chain.mkString(", in ")

  def fail(msg: String): Nothing = throw new SemanticParsingException(s"$msg in $this")
}

object SemanticContext {
  val empty: SemanticContext = SemanticContext(Nil)
}
