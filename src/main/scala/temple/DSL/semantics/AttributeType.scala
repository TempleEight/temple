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

  case class IntType private (
    max: Option[Long] = None,
    min: Option[Long] = None,
    precision: Byte = IntType.defaultPrecision,
  ) extends AttributeType

  object IntType {
    private val defaultPrecision: Byte = 4

    def apply(max: Option[Long] = None, min: Option[Long] = None, precision: Byte = defaultPrecision): IntType =
      if (min.isEmpty && max.fold(false)(_ > 0)) new IntType(max, Some(0), precision)
      else new IntType(max, min, precision)
  }

  case class FloatType(
    max: Option[Double] = None,
    min: Option[Double] = None,
    precision: Byte = FloatType.defaultPrecision,
  ) extends AttributeType

  object FloatType {
    private val defaultPrecision: Byte = 8

    def apply(max: Option[Double] = None, min: Option[Double] = None, precision: Byte = defaultPrecision): FloatType =
      if (min.isEmpty && max.fold(false)(_ > 0)) new FloatType(max, Some(0), precision)
      else new FloatType(max, min, precision)
  }
}
