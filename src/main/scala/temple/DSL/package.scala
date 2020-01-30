package temple

package object DSL {
  // indent a multi-line string: insert two spaces at the start of the string and immediately following newline chars
  // TODO: use global utils once merged
  private def indentString(str: String, length: Int = 2): String = str.replaceAll("^|(?<=\n)", " " * length)

  case class FieldType(typeName: String, args: List[Arg] = List(), kwargs: List[(String, Arg)] = List()) {
    override def toString: String = {
      val kwargsStr = kwargs.map { case (str, arg) => s"$str: $arg" }
      val argsStr   = if (args.isEmpty && kwargs.isEmpty) "" else (args ++ kwargsStr).mkString("(", ",", ")")
      typeName + argsStr
    }
  }
// TODO: move to the next step

//  object FieldType {
//    case class FieldType(typeName: String, args: List[Arg], kwargs: List[(String, Arg)]) extends FieldType
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

  sealed case class Annotation(key: String)

  sealed trait Arg

  object Arg {
    case class TokenArg(name: String)     extends Arg { override def toString: String = name                           }
    case class IntArg(value: Int)         extends Arg { override def toString: String = value.toString                 }
    case class FloatingArg(value: Double) extends Arg { override def toString: String = value.toString                 }
    case class ListArg(elems: List[Arg])  extends Arg { override def toString: String = elems.mkString("[", ", ", "]") }
  }

  sealed trait Entry
  sealed trait ServiceEntry extends Entry

  object Entry {
    case class Attribute(key: String, fieldType: FieldType, annotations: List[Annotation]) extends ServiceEntry {
      override def toString: String = s"$key: $fieldType${annotations.map(" " + _).mkString};"
    }

    case class Metadata(function: String, args: List[Arg], kwargs: List[(String, Arg)]) extends Entry {
      override def toString: String = {
        val kwargsStr = kwargs.map { case (str, arg) => s"$str: $arg" }
        s"#$function ${(args ++ kwargsStr).mkString("(", ", ", ")")};"
      }
    }
  }

  case class DSLRootItem(key: String, tag: String, entries: List[Entry]) extends Entry {
    override def toString: String = {
      val contents = indentString(entries.mkString("\n"))
      s"$key: $tag {\n$contents\n}"
    }
  }

}
