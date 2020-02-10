package temple.generate.database

/** Case class that encapsulates which type of prepared statement placeholder to use in generation */
sealed trait PreparedType

object PreparedType {
  case object QuestionMarks extends PreparedType
  case object DollarNumbers extends PreparedType
}
