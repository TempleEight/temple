package temple.DSL.semantics

case class Attribute(
  attributeType: AttributeType,
  accessAnnotation: Option[Annotation.AccessAnnotation],
  valueAnnotations: Set[Annotation.ValueAnnotation]
)
