package temple.DSL.semantics

import temple.errors.ErrorHandlingContext

final case class SemanticContext private (private val chain: List[String]) extends ErrorHandlingContext {
  def :+(string: String): SemanticContext = SemanticContext(string :: chain)

  override def toString: String = chain.mkString(", in ")

  private def location: String = if (chain.nonEmpty) s" in $this" else ""

  def errorMessage(msg: String): String = msg + location

  def fail(msg: String): Nothing = throw new SemanticParsingException(errorMessage(msg))

}

object SemanticContext {
  val empty: SemanticContext = SemanticContext(Nil)
}
