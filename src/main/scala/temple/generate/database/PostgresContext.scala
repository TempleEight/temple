package temple.generate.database

case class PostgresContext(preparedType: PreparedType) extends DatabaseContext

sealed trait PreparedType

object PreparedType {
  case object QuestionMarks extends PreparedType
  case object DollarNumbers extends PreparedType
}
