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
object PostgresGenerator extends DatabaseGenerator {
  type Context = PostgresContext

  /** Given an expression, parse it into the Postgres format */
  private def generateExpression(expression: Expression, index: Int)(implicit context: PostgresContext): (String, Int) =
    expression match {
      case Value(value) =>
        (value, index)
      case PreparedValue =>
        val value = context.preparedType match {
          case PreparedType.QuestionMarks => "?"
          case PreparedType.DollarNumbers => "$" + index
        }
        (value, index + 1)
    }

  /** Given a list of assignments, parse it into the Postgres format */
  private def generateAssignmentString(
    assignments: Seq[Assignment],
    startIndex: Int,
  )(implicit context: PostgresContext): (String, Int) = {
    val (assignmentStrs, nextIndex) = assignments
      .foldLeft((Seq[String](), startIndex)) {
        case ((seq, index), assignment) =>
          val (expression, nextIndex) = generateExpression(assignment.expression, index)
          (seq :+ s"${assignment.column.name} = $expression", nextIndex)
      }
    (mkCode.list(assignmentStrs), nextIndex)
  }

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
  private def generateCondition(condition: Condition, index: Int)(implicit context: PostgresContext): (String, Int) =
    condition match {
      case Comparison(left, comp, right) =>
        (mkCode(left, generateComparison(comp), right), index)
      case PreparedComparison(left, comp) =>
        context.preparedType match {
          case PreparedType.QuestionMarks => (mkCode(left, generateComparison(comp), "?"), index + 1)
          case PreparedType.DollarNumbers => (mkCode(left, generateComparison(comp), "$" + index), index + 1)
        }
      case Inverse(IsNull(column)) =>
        (mkCode(column.name, "IS NOT NULL"), index)
      case Inverse(condition) =>
        val (genCondition, nextIndex) = generateCondition(condition, index)
        (mkCode("NOT", CodeWrap.parens(genCondition)), nextIndex)
      case IsNull(column) =>
        (mkCode(column.name, "IS NULL"), index)
      case Disjunction(left, right) =>
        val (leftCondition, leftIndex)   = generateCondition(left, index)
        val (rightCondition, rightIndex) = generateCondition(right, leftIndex)
        (mkCode(CodeWrap.parens(leftCondition), "OR", CodeWrap.parens(rightCondition)), rightIndex)
      case Conjunction(left, right) =>
        val (leftCondition, leftIndex)   = generateCondition(left, index)
        val (rightCondition, rightIndex) = generateCondition(right, leftIndex)
        (mkCode(CodeWrap.parens(leftCondition), "AND", CodeWrap.parens(rightCondition)), rightIndex)
    }

  /** Given conditions, generate a Postgres WHERE clause  */
  private def generateConditionString(conditions: Option[Condition], startIndex: Int)(
    implicit context: PostgresContext,
  ): Option[String] =
    conditions.map(generateCondition(_, startIndex)).map { case (conditions, _) => mkCode("WHERE", conditions) }

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
  private def generatePreparedValues(columns: Seq[Any])(implicit context: PostgresContext): String =
    // Make a comma-separated list
    mkCode.list(
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
        val stringColumns = mkCode.spacedList(columns.map(generateColumnDef))
        mkCode.stmt("CREATE TABLE", tableName, CodeWrap.parens.spaced(stringColumns))
      case Read(tableName, columns, conditions) =>
        val stringColumns    = mkCode.list(columns.map(_.name))
        val stringConditions = generateConditionString(conditions, 1)
        mkCode.stmt("SELECT", stringColumns, "FROM", tableName, stringConditions)
      case Insert(tableName, columns, returnColumns) =>
        val stringColumns       = mkCode.list(columns.map(_.name))
        val values              = generatePreparedValues(columns)
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
        val (stringAssignments, nextIndex) = generateAssignmentString(assignments, 1)
        val stringConditions               = generateConditionString(conditions, nextIndex)
        val stringReturnColumns            = generateReturningString(returnColumns)
        mkCode.stmt("UPDATE", tableName, "SET", stringAssignments, stringConditions, stringReturnColumns)
      case Delete(tableName, conditions) =>
        val stringConditions = generateConditionString(conditions, 1)
        mkCode.stmt("DELETE FROM", tableName, stringConditions)
      case Drop(tableName, ifExists) =>
        mkCode.stmt("DROP TABLE", tableName, when(ifExists)("IF EXISTS"))
    }
}
