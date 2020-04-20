package temple.ast

import temple.ast.AbstractAttribute.{Attribute, IDAttribute}
import temple.ast.Metadata.{Endpoint, Omit, ServiceMetadata}

sealed trait AbstractServiceBlock extends AttributeBlock[ServiceMetadata] {
  def attributes: Map[String, AbstractAttribute]
  def metadata: Seq[ServiceMetadata]
  def structs: Map[String, StructBlock]

  /**
    * Flatten a service block into a sequence of structs, including the service's root struct.
    *
    * @param rootName The name of the root struct, typically the name of the service.
    * @return An iterator of pairs of names and struct blocks
    */
  def structIterator(rootName: String): Iterator[(String, AttributeBlock[_])] =
    Iterator(rootName -> this) ++ structs

  /** Return this service's attributes, minus the ID and CreatedBy */
  lazy val providedAttributes: Map[String, Attribute] = attributes.collect {
    case (name: String, x: Attribute) => name -> x
  }

  /** Return the attributes of this service that are stored */
  lazy val storedAttributes: Map[String, AbstractAttribute] = attributes.filter {
    case (_, attribute) => attribute.isStored
  }

  /** Set the parent that this Templefile is within */
  override private[temple] def setParent(parent: TempleNode): Unit = {
    super.setParent(parent)
    structs.valuesIterator.foreach(_.setParent(this))
  }
}

object AbstractServiceBlock {

  /** A service block, representing one microservice on its own isolated server */
  case class ServiceBlock(
    attributes: Map[String, AbstractAttribute],
    metadata: Seq[ServiceMetadata] = Nil,
    structs: Map[String, StructBlock] = Map.empty,
  ) extends AbstractServiceBlock

  case object AuthServiceBlock extends AbstractServiceBlock {

    override val attributes: Map[String, AbstractAttribute] = Map(
      "id"       -> IDAttribute,
      "email"    -> Attribute(AttributeType.StringType(), valueAnnotations = Set(Annotation.Unique)),
      "password" -> Attribute(AttributeType.StringType()),
    )

    override val metadata                          = Seq(Omit(Set(Endpoint.Update, Endpoint.Delete)))
    override val structs: Map[String, StructBlock] = Map()
  }
}
