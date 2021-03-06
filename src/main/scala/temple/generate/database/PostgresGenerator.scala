package temple.generate.database

import temple.generate.database.ast.ColType._
import temple.generate.database.ast.ColumnConstraint._
import temple.generate.database.ast.ComparisonOperator._
import temple.generate.database.ast.Condition._
import temple.generate.database.ast.Expression._
import temple.generate.database.ast.Statement._
import temple.generate.database.ast._
import temple.generate.utils.CodeTerm._

import scala.Option.when

/** Implementation of [[DatabaseGenerator]] for generating PostgreSQL */
private class PostgresGenerator private (context: PostgresContext) {

  private var currentIndex = 1

  private def nextPreparedSymbol(): String = {
    val symbol = context.preparedType match {
      case PreparedType.QuestionMarks => "?"
      case PreparedType.DollarNumbers => "$" + currentIndex
    }
    currentIndex += 1
    symbol
  }

  /** Given an expression, parse it into the Postgres format */
  private def generateExpression(expression: Expression): String =
    expression match {
      case Value(value)  => value
      case PreparedValue => nextPreparedSymbol()
    }

  /** Given a list of assignments, parse it into the Postgres format */
  private def generateAssignmentString(assignments: Seq[Assignment]): String =
    mkCode.list(
      assignments
        .map(assignment => s"${assignment.column.name} = ${generateExpression(assignment.expression)}"),
    )

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
      case Check(left, comp, right)   => mkCode("CHECK", CodeWrap.parens(left, generateComparison(comp), right))
      case Unique                     => "UNIQUE"
      case PrimaryKey                 => "PRIMARY KEY"
      case References(table, colName) => mkCode("REFERENCES", table, CodeWrap.parens(colName))
    }

  /** Given a query modifier, generate the type required by PostgreSQL */
  private def generateCondition(condition: Condition): String =
    condition match {
      case Comparison(left, comp, right)  => mkCode(left, generateComparison(comp), right)
      case PreparedComparison(left, comp) => mkCode(left, generateComparison(comp), nextPreparedSymbol())
      case Inverse(IsNull(column))        => mkCode(column.name, "IS NOT NULL")
      case Inverse(condition)             => mkCode("NOT", CodeWrap.parens(generateCondition(condition)))
      case IsNull(column)                 => mkCode(column.name, "IS NULL")
      case Disjunction(left, right) =>
        mkCode(CodeWrap.parens(generateCondition(left)), "OR", CodeWrap.parens(generateCondition(right)))
      case Conjunction(left, right) =>
        mkCode(CodeWrap.parens(generateCondition(left)), "AND", CodeWrap.parens(generateCondition(right)))
    }

  /** Given conditions, generate a Postgres WHERE clause  */
  private def generateConditionString(conditions: Option[Condition]): Option[String] =
    conditions.map(generateCondition).map(mkCode("WHERE", _))

  private def generateReturningString(returnCols: Seq[Column]): String =
    if (returnCols.isEmpty) ""
    else returnCols.map(_.name).mkString("RETURNING ", ", ", "")

  /** Given a column type, parse it into the type required by PostgreSQL */
  private def generateColumnType(columnType: ColType): String =
    columnType match {
      case IntCol(p) if p <= 2    => "SMALLINT"
      case IntCol(p) if p <= 4    => "INT"
      case IntCol(_)              => "BIGINT"
      case FloatCol(p) if p <= 4  => "REAL"
      case FloatCol(_)            => "DOUBLE PRECISION"
      case BoundedStringCol(size) => s"VARCHAR($size)"
      case StringCol              => "TEXT"
      case BoolCol                => "BOOLEAN"
      case DateCol                => "DATE"
      case TimeCol                => "TIME"
      case DateTimeTzCol          => "TIMESTAMPTZ"
      case BlobCol                => "BYTEA"
      case UUIDCol                => "UUID"
    }

  /** Parse a given column into PostgreSQL syntax */
  private def generateColumnDef(column: ColumnDef): String = {
    val columnConstraints = column.constraints.map(generateConstraint)
    mkCode(column.name, generateColumnType(column.colType), columnConstraints)
  }

  /** Given the current PostgresContext, generate the prepared statement placeholders for each column */
  private def generatePreparedValues(expressions: Seq[Expression]): String =
    // Make a comma-separated list
    mkCode.list(
      expressions.map {
        case Value(value)             => value
        case Expression.PreparedValue => nextPreparedSymbol()
      },
    )

  /** Given a statement, parse it into a valid PostgreSQL statement */
  def generate(statement: Statement): String =
    statement match {
      case Create(tableName, columns) =>
        val stringColumns = mkCode.spacedList(columns.map(generateColumnDef))
        mkCode.stmt("CREATE TABLE", tableName, CodeWrap.parens.spaced(stringColumns))
      case Read(tableName, columns, conditions) =>
        val stringColumns    = mkCode.list(columns.map(_.name))
        val stringConditions = generateConditionString(conditions)
        mkCode.stmt("SELECT", stringColumns, "FROM", tableName, stringConditions)
      case Insert(tableName, assignments, returnColumns) =>
        val stringColumns       = mkCode.list(assignments.map(_.column.name))
        val values              = generatePreparedValues(assignments.map(_.expression))
        val stringReturnColumns = generateReturningString(returnColumns)
        mkCode.stmt(
          "INSERT INTO",
          tableName,
          CodeWrap.parens(stringColumns),
          "VALUES",
          CodeWrap.parens(values),
          stringReturnColumns,
        )
      case Update(tableName, assignments, conditions, returnColumns) =>
        val stringAssignments   = generateAssignmentString(assignments)
        val stringConditions    = generateConditionString(conditions)
        val stringReturnColumns = generateReturningString(returnColumns)
        mkCode.stmt("UPDATE", tableName, "SET", stringAssignments, stringConditions, stringReturnColumns)
      case Delete(tableName, conditions) =>
        val stringConditions = generateConditionString(conditions)
        mkCode.stmt("DELETE FROM", tableName, stringConditions)
      case Drop(tableName, ifExists) =>
        mkCode.stmt("DROP TABLE", tableName, when(ifExists)("IF EXISTS"))
    }
}

object PostgresGenerator extends DatabaseGenerator {
  type Context = PostgresContext

  override def generate(statement: Statement)(implicit context: PostgresContext): String =
    new PostgresGenerator(context).generate(statement)
}
