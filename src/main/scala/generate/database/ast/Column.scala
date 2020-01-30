package generate.database.ast

/** AST implementation of database columns for all data types supported in Templefile */
sealed trait Column {
  val colName: String
}

case class IntColumn(name: String) extends Column {
  override val colName: String = name
}

case class FloatColumn(name: String) extends Column {
  override val colName: String = name
}

case class StringColumn(name: String) extends Column {
  override val colName: String = name
}

case class BoolColumn(name: String) extends Column {
  override val colName: String = name
}

case class DateColumn(name: String) extends Column {
  override val colName: String = name
}

case class TimeColumn(name: String) extends Column {
  override val colName: String = name
}

case class DateTimeTzColumn(name: String) extends Column {
  override val colName: String = name
}
