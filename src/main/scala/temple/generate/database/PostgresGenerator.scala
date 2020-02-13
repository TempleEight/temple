package temple.generate.database

import temple.generate.utils.CodeFormatter._
import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.ComparisonOperator._
import temple.generate.database.ast.Condition._
import temple.generate.database.ast.Expression._
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._

import scala.Option.when

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
object PostgresGenerator extends DatabaseGenerator[PostgresContext] {

  /** Given an expression, parse it into the Postgres format */
  private def generateExpression(expression: Expression): String =
    expression match {
      case Value(value) => value
    }

  /** Given an assignment, parse it into the Postgres format */
  private def generateAssignment(assignment: Assignment): String =
    s"${assignment.column.name} = ${generateExpression(assignment.expression)}"

  /** Given a comparison, parse it into the Postgres format */
  private def generateComparison(comparison: ComparisonOperator): String =
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
      case Check(left, comp, right)   => mkSQL("CHECK", sqlParens(left, generateComparison(comp), right))
      case Unique                     => "UNIQUE"
      case PrimaryKey                 => "PRIMARY KEY"
      case References(table, colName) => mkSQL("REFERENCES", table, sqlParens(colName))
    }

  /** Given a query modifier, generate the type required by PostgreSQL */
  private def generateCondition(condition: Condition): String =
    condition match {
      case Comparison(left, comp, right) => mkSQL(left, generateComparison(comp), right)
      case Inverse(IsNull(column))       => mkSQL(column.name, "IS NOT NULL")
      case Inverse(condition)            => mkSQL("NOT", sqlParens(generateCondition(condition)))
      case IsNull(column)                => mkSQL(column.name, "IS NULL")

      case Disjunction(left, right) =>
        mkSQL(sqlParens(generateCondition(left)), "OR", sqlParens(generateCondition(right)))
      case Conjunction(left, right) =>
        mkSQL(sqlParens(generateCondition(left)), "AND", sqlParens(generateCondition(right)))
    }

  /** Given conditions, generate a Postgres WHERE clause  */
  private def generateConditionString(conditions: Option[Condition]): Option[String] =
    conditions.map(generateCondition).map(mkSQL("WHERE", _))

  /** Given a column type, parse it into the type required by PostgreSQL */
  private def generateColumnType(columnType: ColType): String =
    columnType match {
      case IntCol        => "INT"
      case FloatCol      => "REAL"
      case StringCol     => "TEXT"
      case BoolCol       => "BOOLEAN"
      case DateCol       => "DATE"
      case TimeCol       => "TIME"
      case DateTimeTzCol => "TIMESTAMPTZ"
    }

  /** Parse a given column into PostgreSQL syntax */
  private def generateColumnDef(column: ColumnDef): String = {
    val columnConstraints = column.constraints.map(generateConstraint)
    mkSQL(column.name, generateColumnType(column.colType), columnConstraints)
  }

  /** Given the current PostgresContext, generate the prepared statement placeholders for each column */
  private def generatePreparedValues(columns: Seq[Any])(implicit context: PostgresContext): String =
    // Make a comma-separated list
    mkSQL.list(
      // Consisting of question marks or numbered question marks
      context.preparedType match {
        case PreparedType.QuestionMarks => Iterator.fill(columns.length)("?")
        case PreparedType.DollarNumbers => (1 to columns.length).map("$" + _)
      },
    )

  /** Given a statement, parse it into a valid PostgreSQL statement */
  override def generate(statement: Statement)(implicit context: PostgresContext): String =
    statement match {
      case Create(tableName, columns) =>
        val stringColumns = mkSQL.spacedList(columns.map(generateColumnDef))
        mkSQL.stmt("CREATE TABLE", tableName, sqlParens.spaced(stringColumns))
      case Read(tableName, columns, conditions) =>
        val stringColumns    = columns.map(_.name).mkString(", ")
        val stringConditions = generateConditionString(conditions)
        mkSQL.stmt("SELECT", stringColumns, "FROM", tableName, stringConditions)
      case Insert(tableName, columns) =>
        val stringColumns = columns.map(_.name).mkString(", ")
        val values        = generatePreparedValues(columns)
        mkSQL.stmt("INSERT INTO", tableName, sqlParens(stringColumns), "VALUES", sqlParens(values))
      case Update(tableName, assignments, conditions) =>
        val stringAssignments = assignments.map(generateAssignment).mkString(", ")
        val stringConditions  = generateConditionString(conditions)
        mkSQL.stmt("UPDATE", tableName, "SET", stringAssignments, stringConditions)
      case Delete(tableName, conditions) =>
        val stringConditions = generateConditionString(conditions)
        mkSQL.stmt("DELETE FROM", tableName, stringConditions)
      case Drop(tableName, ifExists) =>
        mkSQL.stmt("DROP TABLE", tableName, when(ifExists)("IF EXISTS"))
    }
}
