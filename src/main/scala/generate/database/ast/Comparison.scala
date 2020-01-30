package generate.database.ast

sealed trait Comparison

case object GreaterEqual extends Comparison

case object Greater extends Comparison

case object Equal extends Comparison

case object NotEqual extends Comparison

case object Less extends Comparison

case object LessEqual extends Comparison
