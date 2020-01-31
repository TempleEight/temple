package temple.generate.database.ast

/** AST Implementation of all query modifiers supported in Templefile */
sealed trait Modifier

object Modifier {
  case class Where(left: String, comparison: Comparison, right: String) extends Modifier
}
