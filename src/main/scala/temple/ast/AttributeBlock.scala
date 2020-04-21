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
}
