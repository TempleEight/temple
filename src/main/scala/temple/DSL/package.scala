package temple

package object DSL {
  sealed trait FieldType

  object FieldType {
    case class SimpleType(typeName: String) extends FieldType
    //    case object BoolType extends FieldType
//
//    case class IntType(max: Long, min: Long)                                        extends FieldType
//    case class StringType(max: Long, min: Long)                                     extends FieldType
//    case class FloatType(max: Double, min: Double, precision: Short)                extends FieldType
//    case class FixedType(max: Double, min: Double, places: Short, precision: Short) extends FieldType
//
//    case class DateType(max: Int, min: Int)     extends FieldType
//    case object TimeType                        extends FieldType
//    case class DateTimeType(max: Int, min: Int) extends FieldType
//    case class BlobType(size: Long)             extends FieldType
//
//    case class ForeignKey() extends FieldType
  }

  sealed case class Annotation(key: String)

  sealed trait Arg

//  object Arg {
//    case class Token(name:String) extends Arg
//    case class Number(value: Int)
//  }

  sealed trait Entry
  sealed trait ServiceEntry extends Entry

  object Entry {
    case class Attribute(key: String, fieldType: FieldType, annotations: List[Annotation]) extends ServiceEntry

    case class Metadata(function: String, args: List[Arg], kwargs: List[(String, Arg)]) extends Entry
    case class NestedService(key: String, entries: List[Attribute])                     extends ServiceEntry
  }

  case class DSLRoot(key: String, tag: String, entries: List[Entry])

}
