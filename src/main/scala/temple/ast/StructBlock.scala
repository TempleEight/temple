package temple.ast

import temple.ast.Metadata.StructMetadata

/** A nested struct within a service, representing a second table/domain in the same datastore */
case class StructBlock(
  attributes: Map[String, Attribute],
  metadata: Seq[StructMetadata] = Nil,
) extends AttributeBlock[StructMetadata]
