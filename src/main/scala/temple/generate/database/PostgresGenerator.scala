package temple.generate.database

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.ComparisonOperator._
import temple.generate.database.ast.Condition._
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._
import temple.utils.StringUtils._

import scala.util.chaining._

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator {

  /** Given a comparison, parse it into the Postgres format */
  private def generateComparison(comparison: ComparisonOperator) =
    comparison match {
      case GreaterEqual => ">="
      case Greater      => ">"
      case Equal        => "="
      case NotEqual     => "<>"
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
  private def generateCondition(condition: Condition): String =
    condition match {
      case Comparison(left, comp, right) => s"$left ${generateComparison(comp)} $right"
      case Disjunction(left, right)      => s"(${generateCondition(left)}) OR (${generateCondition(right)})"
      case Conjunction(left, right)      => s"(${generateCondition(left)}) AND (${generateCondition(right)})"
      case Inverse(IsNull(column))       => s"${column.name} IS NOT NULL"
      case Inverse(condition)            => s"NOT (${generateCondition(condition)})"
      case IsNull(column)                => s"${column.name} IS NULL"
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
      case Read(tableName, columns, conditions) =>
        val stringColumns = columns.map(_.name).mkString(", ")
        val stringConditions = conditions.map(generateCondition) match {
          case Some(conditionsString) => List(s"WHERE $conditionsString")
          case None                   => List()
        }
        (s"SELECT $stringColumns FROM $tableName" +: stringConditions).mkString("", " ", ";")
      case Insert(tableName, columns) =>
        val stringColumns = columns.map(_.name).mkString(", ")
        val values        = (1 to columns.length).map(i => s"$$$i").mkString(", ")
        s"INSERT INTO $tableName ($stringColumns)\nVALUES ($values);"
      case Delete(tableName, conditions) =>
        val stringConditions = conditions.map(generateCondition) match {
          case Some(conditionsString) => List(s"WHERE $conditionsString")
          case None                   => List()
        }
        (s"DELETE FROM $tableName" +: stringConditions).mkString("", " ", ";")
      case Drop(tableName, ifExists) => s"DROP TABLE $tableName" + { if (ifExists) " IF EXISTS;" else ";" }
    }
}
