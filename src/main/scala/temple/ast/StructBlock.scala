package temple.ast

import temple.ast.Metadata.StructMetadata

trait StructBlock[M >: StructMetadata <: Metadata] extends TempleBlock[M] {
  def attributes: Map[String, Attribute]
  def metadata: Seq[M]
}
