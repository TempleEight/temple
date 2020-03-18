package temple.ast

case class Attribute(
  attributeType: AttributeType,
  accessAnnotation: Option[Annotation.AccessAnnotation] = None,
  valueAnnotations: Set[Annotation.ValueAnnotation] = Set.empty,
)
