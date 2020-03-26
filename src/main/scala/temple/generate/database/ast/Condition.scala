package temple.generate.database.ast

/** AST Implementation of all boolean conditions supported in Templefile */
sealed trait Condition

object Condition {
  case class Comparison(left: String, comparison: ComparisonOperator, right: String) extends Condition
  case class PreparedComparison(left: String, comparison: ComparisonOperator)        extends Condition
  case class Conjunction(left: Condition, right: Condition)                          extends Condition
  case class Disjunction(left: Condition, right: Condition)                          extends Condition
  case class Inverse(condition: Condition)                                           extends Condition
  case class IsNull(column: Column)                                                  extends Condition
}
