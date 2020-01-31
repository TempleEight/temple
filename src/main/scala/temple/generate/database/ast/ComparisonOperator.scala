package temple.generate.database.ast

/** AST representation for boolean comparisons */
sealed trait ComparisonOperator

object ComparisonOperator {
  case object GreaterEqual extends ComparisonOperator
  case object Greater      extends ComparisonOperator
  case object Equal        extends ComparisonOperator
  case object NotEqual     extends ComparisonOperator
  case object Less         extends ComparisonOperator
  case object LessEqual    extends ComparisonOperator
}
