package temple.DSL.Semantics

import java.time.{LocalDate, OffsetDateTime}

trait AttributeType {}

object AttributeType {
  case object BoolType                                                           extends AttributeType
  case class StringType(max: Option[Long], min: Int)                             extends AttributeType
  case class IntType(max: Option[Long], min: Option[Long], precision: Short = 4) extends AttributeType
  case class FloatType(max: Double, min: Double, precision: Short = 8)           extends AttributeType
  case object DateType                                                           extends AttributeType
  case object DateTimeType                                                       extends AttributeType
  case object TimeType                                                           extends AttributeType
  case class BlobType(size: Option[Long])                                        extends AttributeType
  case class ForeignKey()                                                        extends AttributeType
}
