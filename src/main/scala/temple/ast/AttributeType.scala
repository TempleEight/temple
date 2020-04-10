package temple.ast

/** The type of an attribute, i.e. a column in a database/field in a struct */
sealed trait AttributeType

object AttributeType {
  case class ForeignKey(references: String) extends AttributeType

  sealed trait PrimitiveAttributeType extends AttributeType

  case object UUIDType     extends PrimitiveAttributeType
  case object BoolType     extends PrimitiveAttributeType
  case object DateType     extends PrimitiveAttributeType
  case object DateTimeType extends PrimitiveAttributeType
  case object TimeType     extends PrimitiveAttributeType

  case class BlobType(size: Option[Long] = None) extends PrimitiveAttributeType

  case class StringType(max: Option[Long] = None, min: Option[Int] = None) extends PrimitiveAttributeType

  case class IntType(max: Option[Long] = None, min: Option[Long] = None, precision: Byte = 4)
      extends PrimitiveAttributeType {

    def precisionMax: Long =
      if (precision >= 8) Long.MaxValue
      else math.pow(2, 8 * precision - 1).toLong - 1

    def precisionMin: Long =
      if (precision >= 8) Long.MaxValue
      else -math.pow(2, 8 * precision - 1).toLong

    def maxValue: Long = max getOrElse precisionMax
    def minValue: Long = min getOrElse precisionMin
  }

  case class FloatType(max: Option[Double] = None, min: Option[Double] = None, precision: Byte = 8)
      extends PrimitiveAttributeType {
    def isDouble: Boolean = precision > 4

    def maxValue: Double = max.getOrElse {
      if (isDouble) Double.MaxValue
      else Float.MaxValue
    }

    def minValue: Double = max.getOrElse {
      if (isDouble) Double.MinValue
      else Float.MinValue
    }
  }
}
