package temple.generate.database

import java.io.InputStream
import java.sql.{Date, Time, Timestamp}

/** Helper case class for holding values for prepared variables in SQL statements
  * Used mainly in @containers.PostgresSpec */
sealed trait PreparedVariable

/** Enumerated case classes for each supported datatype in JDBC */
object PreparedVariable {
  case class IntVariable(value: Int)              extends PreparedVariable
  case class StringVariable(value: String)        extends PreparedVariable
  case class FloatVariable(value: Float)          extends PreparedVariable
  case class BoolVariable(value: Boolean)         extends PreparedVariable
  case class DateVariable(value: Date)            extends PreparedVariable
  case class TimeVariable(value: Time)            extends PreparedVariable
  case class DateTimeTzVariable(value: Timestamp) extends PreparedVariable
  case class BlobVariable(value: Array[Byte])     extends PreparedVariable
}
