package generate.database.ast

/** AST implementation of all the column constraints supported in Templefile */
sealed trait ColumnConstraints

case class NonNull() extends ColumnConstraints

case class Null() extends ColumnConstraints

case class Check() extends ColumnConstraints

case class Unique() extends ColumnConstraints

case class PrimaryKey() extends ColumnConstraints

case class References() extends ColumnConstraints
