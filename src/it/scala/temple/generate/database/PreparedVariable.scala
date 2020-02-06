package temple.generate.database

import java.sql.{Date, Time, Timestamp}

sealed trait PreparedVariable

object PreparedVariable {
  case class IntVariable(value: Int)              extends PreparedVariable
  case class StringVariable(value: String)        extends PreparedVariable
  case class FloatVariable(value: Float)          extends PreparedVariable
  case class BoolVariable(value: Boolean)         extends PreparedVariable
  case class DateVariable(value: Date)            extends PreparedVariable
  case class TimeVariable(value: Time)            extends PreparedVariable
  case class DateTimeTzVariable(value: Timestamp) extends PreparedVariable
}
