package generate.database.ast

/** AST implementation of database columns for all data types supported in Templefile */
sealed trait Column {
  def name: String
}

case class IntColumn(name: String) extends Column

case class FloatColumn(name: String) extends Column

case class StringColumn(name: String) extends Column

case class BoolColumn(name: String) extends Column

case class DateColumn(name: String) extends Column

case class TimeColumn(name: String) extends Column

case class DateTimeTzColumn(name: String) extends Column
