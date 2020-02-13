package temple.DSL.semantics

import temple.DSL.semantics.Metadata.ServiceMetadata

/** A service block, representing one microservice on its own isolated server */
case class ServiceBlock(
  attributes: Map[String, Attribute],
  metadata: Seq[ServiceMetadata] = Nil,
  structs: Map[String, StructBlock] = Map.empty,
)