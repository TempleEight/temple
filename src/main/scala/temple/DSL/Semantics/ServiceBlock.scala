package temple.DSL.Semantics

import temple.DSL.Semantics.Metadata.ServiceMetadata

/** A service block, representing one microservice on its own isolated server */
case class ServiceBlock(
  attributes: Map[String, Attribute],
  metadata: List[ServiceMetadata],
  structs: Map[String, StructBlock]
)
