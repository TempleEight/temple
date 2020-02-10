package temple.DSL.Syntax

/** Any element of a service/struct */
trait Entry

object Entry {

  case class Attribute(key: String, dataType: AttributeType, annotations: Seq[Annotation] = Nil) extends Entry {
    override def toString: String = s"$key: $dataType${annotations.map(" " + _).mkString};"
  }

  case class Metadata(metaKey: String, args: Args = Args()) extends Entry {
    override def toString: String = s"#$metaKey ($args);"
  }
}
