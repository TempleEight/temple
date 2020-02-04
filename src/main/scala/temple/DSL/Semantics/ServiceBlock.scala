package temple.DSL.Semantics

case class ServiceBlock(
  attributes: Map[String, AttributeType],
  metadata: List[ServiceMetadata],
  structs: Map[String, StructBlock]
)
