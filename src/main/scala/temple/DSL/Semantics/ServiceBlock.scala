package temple.DSL.Semantics

case class ServiceBlock(
  attributes: Map[String, AttributeType],
  metadata: List[ServiceMetadata],
  subservice: Map[String, SubService]
)
