package temple.ast

import temple.ast.Metadata.{Endpoint, Omit, ServiceMetadata}

case object AuthServiceBlock extends GeneratedBlock {

  override def attributes: Map[String, Attribute] = Map(
    "id"       -> Attribute(AttributeType.UUIDType, valueAnnotations = Set(Annotation.Unique)),
    "email"    -> Attribute(AttributeType.StringType(), valueAnnotations = Set(Annotation.Unique)),
    "password" -> Attribute(AttributeType.StringType()),
  )

  override def metadata                          = Seq(Omit(Set(Endpoint.Update, Endpoint.Delete)))
  override def structs: Map[String, StructBlock] = Map()
}
