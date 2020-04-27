package temple.ast

import temple.ast.Annotation.{AccessAnnotation, ValueAnnotation}

sealed trait AbstractAttribute {
  def attributeType: AttributeType
  def accessAnnotation: Option[AccessAnnotation]
  def valueAnnotations: Set[ValueAnnotation]

  def inRequest: Boolean =
    !accessAnnotation.contains(Annotation.Server) && !accessAnnotation.contains(Annotation.ServerSet)

  def inResponse: Boolean =
    !accessAnnotation.contains(Annotation.Server) && !accessAnnotation.contains(Annotation.Client)

  def isStored: Boolean =
    !accessAnnotation.contains(Annotation.Client)
}

object AbstractAttribute {

  case class Attribute(
    attributeType: AttributeType,
    accessAnnotation: Option[AccessAnnotation] = None,
    valueAnnotations: Set[ValueAnnotation] = Set.empty,
  ) extends AbstractAttribute

  case object IDAttribute extends AbstractAttribute {
    override def attributeType: AttributeType               = AttributeType.UUIDType
    override def accessAnnotation: Option[AccessAnnotation] = Some(Annotation.ServerSet)
    override def valueAnnotations: Set[ValueAnnotation]     = Set()
  }

  case object ParentAttribute extends AbstractAttribute {
    override def attributeType: AttributeType               = AttributeType.UUIDType
    override def accessAnnotation: Option[AccessAnnotation] = Some(Annotation.Server)
    override def valueAnnotations: Set[ValueAnnotation]     = Set()
  }

  case object CreatedByAttribute extends AbstractAttribute {
    override def attributeType: AttributeType               = AttributeType.UUIDType
    override def accessAnnotation: Option[AccessAnnotation] = Some(Annotation.Server)
    override def valueAnnotations: Set[ValueAnnotation]     = Set()
  }
}
