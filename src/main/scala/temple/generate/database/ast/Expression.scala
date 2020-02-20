package temple.generate.database.ast

/** AST representation for expressions */
sealed trait Expression

object Expression {
  case class Value(value: String) extends Expression
}
