package generate.database

import generate.database.ast._
import utils.StringUtils._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  private def parseComparison(comparison: Comparison): String =
    comparison match {
      case GreaterEqual => ">="
      case Greater      => ">"
      case Equal        => "=="
      case NotEqual     => "!="
      case Less         => "<"
      case LessEqual    => "<="
    }

  private def parseConstraint(constraint: ColumnConstraint): String =
    constraint match {
      case NonNull                    => "NOT NULL"
      case Null                       => "NULL"
      case Check(left, comp, right)   => s"CHECK ($left " + parseComparison(comp) + s" $right)"
      case Unique                     => "UNIQUE"
      case PrimaryKey                 => "PRIMARY KEY"
      case References(table, colName) => s"REFERENCES $table($colName)"
    }

  /** Given a column, parse it into the type required by Postgres */
  private def parseColumn(column: Column): String =
    indent(
      column match {
        case IntColumn(name, constraints)    => s"$name INT" + constraints.map(parseConstraint).map(" " + _).mkString
        case FloatColumn(name, constraints)  => s"$name REAL" + constraints.map(parseConstraint).map(" " + _).mkString
        case StringColumn(name, constraints) => s"$name TEXT" + constraints.map(parseConstraint).map(" " + _).mkString
        case BoolColumn(name, constraints)   => s"$name BOOLEAN" + constraints.map(parseConstraint).map(" " + _).mkString
        case DateColumn(name, constraints)   => s"$name DATE" + constraints.map(parseConstraint).map(" " + _).mkString
        case TimeColumn(name, constraints)   => s"$name TIME" + constraints.map(parseConstraint).map(" " + _).mkString
        case DateTimeTzColumn(name, constraints) =>
          s"$name TIMESTAMPTZ" + constraints.map(parseConstraint).map(" " + _).mkString
      },
      length = 4
    )

  /** Given a statement, parse it into a valid PostgresQL statement */
  override def generate(statement: Statement): String = {
    val sb = new StringBuilder()
    statement match {
      case Create(tableName, columns) =>
        sb.append(s"CREATE TABLE $tableName ")
        val stringColumns = columns map parseColumn
        sb.append(stringColumns.mkString("(\n", ",\n", "\n)"))
        sb.append(";\n")
    }
    sb.mkString
  }
}
