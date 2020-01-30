package generate.database.ast

/** AST implementation of database columns for all data types supported in Templefile */
sealed trait Column

case class IntColumn(name: String, constraints: List[ColumnConstraint] = List()) extends Column

case class FloatColumn(name: String, constraints: List[ColumnConstraint] = List()) extends Column

case class StringColumn(name: String, constraints: List[ColumnConstraint] = List()) extends Column

case class BoolColumn(name: String, constraints: List[ColumnConstraint] = List()) extends Column

case class DateColumn(name: String, constraints: List[ColumnConstraint] = List()) extends Column

case class TimeColumn(name: String, constraints: List[ColumnConstraint] = List()) extends Column

case class DateTimeTzColumn(name: String, constraints: List[ColumnConstraint] = List()) extends Column
