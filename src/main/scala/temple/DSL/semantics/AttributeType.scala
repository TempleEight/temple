package temple.DSL.semantics

/** The type of an attribute, i.e. a column in a database/field in a struct */
trait AttributeType

object AttributeType {
  case object ForeignKey   extends AttributeType
  case object BoolType     extends AttributeType
  case object DateType     extends AttributeType
  case object DateTimeType extends AttributeType
  case object TimeType     extends AttributeType

  case class BlobType(size: Option[Long] = None)                           extends AttributeType
  case class StringType(max: Option[Long] = None, min: Option[Int] = None) extends AttributeType

  case class IntType(max: Option[Long] = None, min: Option[Long] = None, precision: Byte = 4) extends AttributeType

  case class FloatType(max: Option[Double] = None, min: Option[Double] = None, precision: Byte = 8)
      extends AttributeType
}
