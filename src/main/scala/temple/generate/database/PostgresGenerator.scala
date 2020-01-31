package temple.generate.database

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.Comparison._
import temple.generate.database.ast.Modifier.Where
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._
import temple.utils.StringUtils._

import scala.util.chaining._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  /** Given a comparison, parse it into the Postgres format */
  private def generateComparison(comparison: Comparison) =
    comparison match {
      case GreaterEqual => ">="
      case Greater      => ">"
      case Equal        => "=="
      case NotEqual     => "!="
      case Less         => "<"
      case LessEqual    => "<="
    }

  /** Given a column constraint, parse it into the Postgres format */
  private def generateConstraint(constraint: ColumnConstraint): String =
    constraint match {
      case NonNull                    => "NOT NULL"
      case Null                       => "NULL"
      case Check(left, comp, right)   => s"CHECK ($left " + generateComparison(comp) + s" $right)"
      case Unique                     => "UNIQUE"
      case PrimaryKey                 => "PRIMARY KEY"
      case References(table, colName) => s"REFERENCES $table($colName)"
    }

  /** Given a query modifier, generate the type required by PostgreSQL */
  private def generateModifier(modifier: Modifier): String =
    modifier match {
      case Where(left, comp, right) => s"WHERE $left ${generateComparison(comp)} $right"
    }

  /** Given a column type, parse it into the type required by PostgreSQL */
  private def generateColumnType(columnType: ColType): String =
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
  private def generateColumnDef(column: ColumnDef): String = {
    val columnConstraints = column.constraints.map(generateConstraint)
    (column.name +: generateColumnType(column.colType) +: columnConstraints).mkString(" ")
  }

  /** Given a statement, parse it into a valid PostgreSQL statement */
  override def generate(statement: Statement): String =
    statement match {
      case Create(tableName, columns) =>
        val stringColumns =
          columns
            .map(generateColumnDef)
            .mkString(",\n")
            .pipe(indent(_))
        s"CREATE TABLE $tableName (\n$stringColumns\n);"
      case Read(tableName, columns, modifiers) =>
        val stringColumns   = columns.map(_.name).mkString(", ")
        val stringModifiers = modifiers.map(generateModifier)
        (s"SELECT $stringColumns FROM $tableName" +: stringModifiers).mkString("", " ", ";")
      case Insert(tableName, columns, modifiers) =>
        val stringColumns   = columns.map(_.name).mkString(", ")
        val stringModifiers = modifiers.map(generateModifier)
        val values          = (for (i <- 1.to(columns.length)) yield s"$$$i").toList.mkString(", ")
        (s"INSERT INTO $tableName ($stringColumns)" +: s"VALUES ($values)" +: stringModifiers).mkString("", "\n", ";")
    }
}
