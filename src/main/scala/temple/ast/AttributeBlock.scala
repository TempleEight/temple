package temple.ast

import temple.ast.AbstractAttribute.Attribute
import temple.ast.Metadata.StructMetadata

/** Either a struct block or a service block */
trait AttributeBlock[M >: StructMetadata <: Metadata] extends TempleBlock[M] {
  def attributes: Map[String, AbstractAttribute]
  def metadata: Seq[M]

  /** Return this service's attributes, minus the ID and CreatedBy */
  lazy val providedAttributes: Map[String, Attribute] = attributes.collect {
    case name -> (attribute: Attribute) => name -> attribute
  }

  /** Return the attributes of this service that are stored */
  lazy val storedAttributes: Map[String, AbstractAttribute] = attributes.filter {
    case (_, attribute) => attribute.isStored
  }
}
