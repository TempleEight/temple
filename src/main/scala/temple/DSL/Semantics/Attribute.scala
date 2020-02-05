package temple.DSL.Semantics

case class Attribute(
  attributeType: AttributeType,
  accessAnnotation: Option[Annotation.AccessAnnotation],
  valueAnnotations: Set[Annotation.ValueAnnotation]
)
