package generate.database

import generate.database.ast._
import utils.StringUtils._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  /** Given a column, parse it into the type required by Postgres */
  private def parseColumn(column: Column): String =
    indent(
      column.colType match {
        case Some(ColType.IntCol)        => s"${column.name} INT"
        case Some(ColType.FloatCol)      => s"${column.name} REAL"
        case Some(ColType.StringCol)     => s"${column.name} TEXT"
        case Some(ColType.BoolCol)       => s"${column.name} BOOLEAN"
        case Some(ColType.DateCol)       => s"${column.name} DATE"
        case Some(ColType.TimeCol)       => s"${column.name} TIME"
        case Some(ColType.DateTimeTzCol) => s"${column.name} TIMESTAMPTZ"
        case None                        => s"${column.name}"
      }
    )

  /** Given a statement, parse it into a valid PostgresQL statement */
  override def generate(statement: Statement): String = {
    val sb = new StringBuilder()
    statement match {
      case Create(tableName, columns) =>
        sb.append(s"CREATE TABLE $tableName ")
        val stringColumns = columns map parseColumn
        sb.append(stringColumns.mkString("(\n", ",\n", "\n)"))
      case Read(columns, tableName) =>
        sb.append("SELECT")
        val stringColumns = columns map parseColumn
        sb.append(stringColumns.mkString("\n", ",\n", "\n"))
        sb.append(s"FROM\n${indent(tableName)}")
    }
    sb.append(";\n")
    sb.mkString
  }
}
