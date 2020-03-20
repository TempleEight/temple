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
      extends PrimitiveAttributeType

  case class FloatType(max: Option[Double] = None, min: Option[Double] = None, precision: Byte = 8)
      extends PrimitiveAttributeType
}
