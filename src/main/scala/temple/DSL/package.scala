package temple

import temple.utils.StringUtils._

/** Handles the internal representation of the DSL of the Templefile */
package object DSL {

  /** The type of a struct’s attribute, complete with parameters */
  case class AttributeType(typeName: String, args: Seq[Arg] = Nil, kwargs: Seq[(String, Arg)] = Nil) {

    override def toString: String = {
      val kwargsStr = kwargs.map { case (str, arg) => s"$str: $arg" }
      val argsStr   = if (args.isEmpty && kwargs.isEmpty) "" else (args ++ kwargsStr).mkString("(", ",", ")")
      typeName + argsStr
    }
  }

// TODO: move to the next step

//  object FieldType {
//    case class FieldType(typeName: String, args: Seq[Arg], kwargs: Seq[(String, Arg)]) extends FieldType
//    //    case object BoolType extends FieldType
////
////    case class IntType(max: Long, min: Long)                                        extends FieldType
////    case class StringType(max: Long, min: Long)                                     extends FieldType
////    case class FloatType(max: Double, min: Double, precision: Short)                extends FieldType
////    case class FixedType(max: Double, min: Double, places: Short, precision: Short) extends FieldType
////
////    case class DateType(max: Int, min: Int)     extends FieldType
////    case object TimeType                        extends FieldType
////    case class DateTimeType(max: Int, min: Int) extends FieldType
////    case class BlobType(size: Long)             extends FieldType
////
////    case class ForeignKey() extends FieldType
//  }

  /** The annotation of a struct’s attribute */
  sealed case class Annotation(key: String)

  /** A value that can be passed as an argument to a type or metadata */
  sealed trait Arg

  object Arg {
    case class TokenArg(name: String)     extends Arg { override def toString: String = name                           }
    case class IntArg(value: scala.Int)   extends Arg { override def toString: String = value.toString                 }
    case class FloatingArg(value: Double) extends Arg { override def toString: String = value.toString                 }
    case class ListArg(elems: Seq[Arg])   extends Arg { override def toString: String = elems.mkString("[", ", ", "]") }
  }

  /** Any element of a service/struct */
  sealed trait Entry

  object Entry {

    case class Attribute(key: String, dataType: AttributeType, annotations: Seq[Annotation]) extends Entry {
      override def toString: String = s"$key: $dataType${annotations.map(" " + _).mkString};"
    }

    case class Metadata(function: String, args: Seq[Arg], kwargs: Seq[(String, Arg)]) extends Entry {

      override def toString: String = {
        val kwargsStr = kwargs.map { case (str, arg) => s"$str: $arg" }
        s"#$function ${(args ++ kwargsStr).mkString("(", ", ", ")")};"
      }
    }
  }

  /** An item at the root of the Templefile, e.g. services and targets */
  case class DSLRootItem(key: String, tag: String, entries: Seq[Entry]) extends Entry {

    override def toString: String = {
      val contents = indent(entries.mkString("\n"))
      s"$key: $tag {\n$contents\n}"
    }
  }
}
