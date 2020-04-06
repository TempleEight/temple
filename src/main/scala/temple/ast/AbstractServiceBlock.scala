package temple.ast

import temple.ast.Metadata.{Endpoint, Omit, ServiceMetadata}

sealed trait AbstractServiceBlock extends TempleBlock[ServiceMetadata] {
  def attributes: Map[String, Attribute]
  def metadata: Seq[ServiceMetadata]
  def structs: Map[String, StructBlock]

  /**
    * Flatten a service block into a sequence of structs, including the service's root struct.
    *
    * @param rootName The name of the root struct, typically the name of the service.
    * @return An iterator of pairs of names and struct blocks,
    *         represented as an iterator of pairs of names and attributes
    */
  def structIterator(rootName: String): Iterator[(String, Map[String, Attribute])] =
    Iterator((rootName, attributes)) ++ structs.iterator.map { case (str, block) => (str, block.attributes) }

  /** Set the parent that this Templefile is within */
  override private[temple] def setParent(parent: TempleNode): Unit = {
    super.setParent(parent)
    structs.valuesIterator.foreach(_.setParent(this))
  }
}

object AbstractServiceBlock {

  /** A service block, representing one microservice on its own isolated server */
  case class ServiceBlock(
    attributes: Map[String, Attribute],
    metadata: Seq[ServiceMetadata] = Nil,
    structs: Map[String, StructBlock] = Map.empty,
  ) extends AbstractServiceBlock

  case object AuthServiceBlock extends AbstractServiceBlock {

    override val attributes: Map[String, Attribute] = Map(
      "id"       -> Attribute(AttributeType.UUIDType, valueAnnotations = Set(Annotation.Unique)),
      "email"    -> Attribute(AttributeType.StringType(), valueAnnotations = Set(Annotation.Unique)),
      "password" -> Attribute(AttributeType.StringType()),
    )

    override val metadata                          = Seq(Omit(Set(Endpoint.Update, Endpoint.Delete)))
    override val structs: Map[String, StructBlock] = Map()
  }
}
