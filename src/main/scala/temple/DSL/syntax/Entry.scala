package temple.DSL.syntax

/** Any element of a service/struct */
abstract class Entry(val typeName: String)

object Entry {

  case class Attribute(key: String, dataType: AttributeType, annotations: Seq[Annotation] = Nil)
      extends Entry("attribute") {
    override def toString: String = s"$key: $dataType${annotations.map(" " + _).mkString};"
  }

  case class Metadata(metaKey: String, args: Args = Args()) extends Entry("metadata") {
    override def toString: String = s"#$metaKey ($args);"
  }
}
