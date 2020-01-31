package generate.database

import generate.database.ast.ColType._
import generate.database.ast._
import utils.StringUtils._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  /** Given a comparison, parse it into the Postgres format */
  private def parseComparison(comparison: Comparison): String =
    comparison match {
      case GreaterEqual => ">="
      case Greater      => ">"
      case Equal        => "=="
      case NotEqual     => "!="
      case Less         => "<"
      case LessEqual    => "<="
    }

  /** Given a column constraint, parse it into the Postgres format */
  private def parseConstraint(constraint: ColumnConstraint): String =
    constraint match {
      case NonNull                    => "NOT NULL"
      case Null                       => "NULL"
      case Check(left, comp, right)   => s"CHECK ($left " + parseComparison(comp) + s" $right)"
      case Unique                     => "UNIQUE"
      case PrimaryKey                 => "PRIMARY KEY"
      case References(table, colName) => s"REFERENCES $table($colName)"
    }

  @inline private def parseColumnConstraints(constraints: List[ColumnConstraint]): String =
    constraints.map(parseConstraint).mkString(" ", " ", "", "")

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
    indent(s"${column.name} ${parseColumnType(column.colType)}${parseColumnConstraints(column.constraints)}")

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
