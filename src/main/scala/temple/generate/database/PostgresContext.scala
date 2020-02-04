package temple.generate.database

case class PostgresContext(preparedType: PreparedType)

sealed trait PreparedType

object PreparedType {
  case object QuestionMarks extends PreparedType
  case object DollarNumbers extends PreparedType
}
