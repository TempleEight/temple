package temple.DSL.semantics

/** The type of an attribute, i.e. a column in a database/field in a struct */
trait AttributeType

object AttributeType {
  case class ForeignKey()  extends AttributeType
  case object BoolType     extends AttributeType
  case object DateType     extends AttributeType
  case object DateTimeType extends AttributeType
  case object TimeType     extends AttributeType

  case class BlobType(size: Option[Long] = None)                extends AttributeType
  case class StringType(max: Option[Long] = None, min: Int = 0) extends AttributeType

  case class IntType(max: Option[Long] = None, min: Option[Long] = None, precision: Short = 4) extends AttributeType

  case class FloatType(max: Double = Double.MaxValue, min: Double = Double.MinValue, precision: Short = 8)
      extends AttributeType
}
