package generate.database

import generate.database.ast._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  /** Given a column, parse it into the type required by Postgres */
  private def parseColumn(column: Column): String =
    column match {
      case IntColumn(name)        => s"    $name INT"
      case FloatColumn(name)      => s"    $name REAL"
      case StringColumn(name)     => s"    $name TEXT"
      case BoolColumn(name)       => s"    $name BOOLEAN"
      case DateColumn(name)       => s"    $name DATE"
      case TimeColumn(name)       => s"    $name TIME"
      case DateTimeTzColumn(name) => s"    $name TIMESTAMPTZ"
    }

  /** Given a statement, parse it into a valid PostgresQL statement */
  override def generate(statement: Statement): String = {
    val sb = new StringBuilder()
    statement match {
      case Create(tableName, columns) =>
        sb.append(s"CREATE TABLE $tableName (\n")
        val stringColumns = columns map parseColumn
        sb.append(stringColumns.mkString("", ",\n", "\n"))
        sb.append(");\n")
    }
    sb.mkString
  }
}
