package temple.DSL.semantics

import temple.DSL.semantics.Metadata.StructMetadata

/** A nested struct within a service, representing a second table/domain in the same datastore */
case class StructBlock(
  attributes: Map[String, Attribute],
  metadata: Seq[StructMetadata] = Nil,
)
