package temple.generate.database

sealed trait PreparedType

object PreparedType {
  case object QuestionMarks extends PreparedType
  case object DollarNumbers extends PreparedType
}
