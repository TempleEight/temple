package temple.DSL.semantics

import temple.errors.Context

case class SemanticContext private (private val chain: List[String]) extends Context {
  def :+(string: String): SemanticContext = SemanticContext(string :: chain)

  override def toString: String = chain.mkString(", in ")

  def fail(msg: String): Nothing = throw new SemanticParsingException(s"$msg in $this")
}

object SemanticContext {
  val empty: SemanticContext = SemanticContext(Nil)
}
