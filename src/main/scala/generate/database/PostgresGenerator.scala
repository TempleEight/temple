package generate.database

import generate.database.ast._
import utils.StringUtils._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  /** Given a column, parse it into the type required by Postgres */
  private def parseColumnDef(column: ColumnDef): String =
    indent(
      column.colType match {
        case ColType.IntCol        => s"${column.name} INT"
        case ColType.FloatCol      => s"${column.name} REAL"
        case ColType.StringCol     => s"${column.name} TEXT"
        case ColType.BoolCol       => s"${column.name} BOOLEAN"
        case ColType.DateCol       => s"${column.name} DATE"
        case ColType.TimeCol       => s"${column.name} TIME"
        case ColType.DateTimeTzCol => s"${column.name} TIMESTAMPTZ"
      }
    )

  /** Given a statement, parse it into a valid PostgresQL statement */
  override def generate(statement: Statement): String = {
    val sb = new StringBuilder()
    statement match {
      case Create(tableName, columns) =>
        sb.append(s"CREATE TABLE $tableName ")
        val stringColumns = columns.map(parseColumnDef)
        sb.append(stringColumns.mkString("(\n", ",\n", "\n)"))
      case Read(tableName, columns) =>
        sb.append("SELECT ")
        val stringColumns = columns.map(_.name)
        sb.append(stringColumns.mkString(", "))
        sb.append(s" FROM $tableName")
    }
    sb.append(";\n")
    sb.mkString
  }
}
