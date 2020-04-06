package temple.ast

import temple.ast.Metadata.StructMetadata

/** Either a struct block or a service block */
trait AttributeBlock[M >: StructMetadata <: Metadata] extends TempleBlock[M] {
  def attributes: Map[String, Attribute]
  def metadata: Seq[M]
}
