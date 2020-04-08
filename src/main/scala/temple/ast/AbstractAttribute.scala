package temple.ast

import temple.ast.Annotation.{AccessAnnotation, ValueAnnotation}

sealed trait AbstractAttribute {
  def attributeType: AttributeType
  def accessAnnotation: Option[AccessAnnotation]
  def valueAnnotations: Set[ValueAnnotation]
}

object AbstractAttribute {

  case class Attribute(
    attributeType: AttributeType,
    accessAnnotation: Option[AccessAnnotation] = None,
    valueAnnotations: Set[ValueAnnotation] = Set.empty,
  ) extends AbstractAttribute

  case object IDAttribute extends AbstractAttribute {
    override def attributeType: AttributeType               = AttributeType.UUIDType
    override def accessAnnotation: Option[AccessAnnotation] = None
    override def valueAnnotations: Set[ValueAnnotation]     = Set()
  }

  case object CreatedByAttribute extends AbstractAttribute {
    override def attributeType: AttributeType               = AttributeType.UUIDType
    override def accessAnnotation: Option[AccessAnnotation] = None
    override def valueAnnotations: Set[ValueAnnotation]     = Set()
  }
}
