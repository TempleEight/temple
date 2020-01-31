package generate.database.ast

/** AST representation for boolean comparisons */
sealed trait Comparison

object Comparison {

  case object GreaterEqual extends Comparison

  case object Greater extends Comparison

  case object Equal extends Comparison

  case object NotEqual extends Comparison

  case object Less extends Comparison

  case object LessEqual extends Comparison

}
