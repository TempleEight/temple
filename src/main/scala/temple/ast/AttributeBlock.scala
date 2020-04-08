package temple.ast

import temple.ast.Metadata.StructMetadata

/** Either a struct block or a service block */
trait AttributeBlock[M >: StructMetadata <: Metadata] extends TempleBlock[M] {
  def attributes: Map[String, AbstractAttribute]
  def metadata: Seq[M]
}
