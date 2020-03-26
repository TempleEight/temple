package temple.DSL.semantics

import temple.ast.AttributeType._
import temple.ast._

private class Validator private (templefile: Templefile) {

  private lazy val allStructs: Set[String] =
    templefile.services.flatMap { case serviceName -> service => Iterator(serviceName) ++ service.structs.keys }.toSet

  private lazy val allServices: Set[String] = templefile.services.keys.toSet

  private def validateService(service: ServiceBlock)(context: SemanticContext): Unit = {
    service.structs.foreachEntry(context(validateStruct))
    service.attributes.foreachEntry(context(validateAttribute))
    service.metadata.foreach(validateMetadata(_, service.attributes)(context))
  }

  private def validateStruct(struct: StructBlock)(context: SemanticContext): Unit = {
    struct.attributes.foreachEntry(context(validateAttribute))
    struct.metadata.foreach(validateMetadata(_, struct.attributes)(context))
  }

  private def validateMetadata(metadata: Metadata, attributes: Map[String, Attribute])(context: SemanticContext): Unit =
    metadata match {
      case _: Metadata.TargetLanguage    => // Nothing to validate yet
      case Metadata.TargetAuth(_)        => context.fail(s"TODO: figure out how this auth works")
      case _: Metadata.ServiceLanguage   => // Nothing to validate yet
      case _: Metadata.Database          => // Nothing to validate yet
      case Metadata.ServiceAuth(_)       => context.fail(s"TODO: figure out how this auth works")
      case _: Metadata.Readable          => // Nothing to validate yet
      case _: Metadata.Writable          => // Nothing to validate yet
      case Metadata.Omit(_)              => // Nothing to validate yet
      case Metadata.ServiceEnumerable(_) => // Nothing to validate yet
      case _: Metadata.Provider          => // Nothing to validate yet
      case Metadata.Uses(services) =>
        for (service <- services if !allServices.contains(service)) context.fail(s"No such service $service")
    }

  private def validateAttribute(attribute: Attribute)(context: SemanticContext): Unit = {
    validateAttributeType(attribute.attributeType)(context)
    attribute.accessAnnotation match {
      case Some(Annotation.Client)    => // nothing to validate
      case Some(Annotation.Server)    => // nothing to validate
      case Some(Annotation.ServerSet) => // nothing to validate
      case None                       => // nothing to validate
    }
  }

  private def validateAttributeType(attributeType: AttributeType)(context: SemanticContext): Unit =
    attributeType match {
      case ForeignKey(references) if !allStructs.contains(references) =>
        context.fail(s"Invalid foreign key $references")
      case UUIDType                                      => context.fail(s"Illegal use of UUID type")
      case BlobType(Some(size)) if size < 0              => context.fail(s"BlobType size is negative")
      case StringType(Some(max), _) if max < 0           => context.fail(s"StringType max is negative")
      case StringType(Some(max), Some(min)) if max < min => context.fail(s"StringType max not above min")
      case IntType(Some(max), Some(min), _) if max < min =>
        context.fail(s"IntType max not above min")
      case IntType(_, _, precision) if precision <= 0 || precision > 8 =>
        context.fail(s"IntType precision not between 0 and 8")
      case FloatType(Some(max), Some(min), _) if max < min =>
        context.fail(s"FloatType max not above min")
      case FloatType(_, _, precision) if precision <= 0 || precision > 8 =>
        context.fail(s"FloatType precision not between 0 and 8")
      case BoolType | DateType | DateTimeType | TimeType => // nothing to check
    }

  def validate(): Unit = templefile.services.foreachEntry(SemanticContext.empty(validateService))
}

object Validator {
  def validate(templefile: Templefile): Unit = new Validator(templefile).validate()
}
