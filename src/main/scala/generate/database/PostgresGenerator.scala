package generate.database

import generate.database.ast._
import generate.database.ast.ColType._
import utils.StringUtils._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  /** Given a column type, parse it into the type required by PostgreSQL */
  private def parseColumnType(columnType: ColType): String =
    columnType match {
      case IntCol        => s"INT"
      case FloatCol      => s"REAL"
      case StringCol     => s"TEXT"
      case BoolCol       => s"BOOLEAN"
      case DateCol       => s"DATE"
      case TimeCol       => s"TIME"
      case DateTimeTzCol => s"TIMESTAMPTZ"
    }

  /** Parse a given column into PostgreSQL syntax */
  private def parseColumnDef(column: ColumnDef): String =
    indent(s"${column.name} ${parseColumnType(column.colType)}")

  /** Given a statement, parse it into a valid PostgreSQL statement */
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
