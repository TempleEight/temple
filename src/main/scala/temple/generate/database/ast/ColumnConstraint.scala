package temple.generate.database.ast

/** AST implementation of all the column constraints supported in Templefile */
sealed trait ColumnConstraint

object ColumnConstraint {
  case object NonNull                                                     extends ColumnConstraint
  case class Check(left: String, comp: ComparisonOperator, right: String) extends ColumnConstraint
  case object Unique                                                      extends ColumnConstraint
  case object PrimaryKey                                                  extends ColumnConstraint
  case class References(table: String, colName: String)                   extends ColumnConstraint
}
