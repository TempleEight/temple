package temple.DSL.semantics

import temple.ast.AttributeType._
import temple.ast.{Metadata, _}

import scala.reflect.{ClassTag, classTag}

private class Validator private (templefile: Templefile) {

  private lazy val allStructs: Iterable[String] = templefile.services.flatMap {
    case (name, service) => service.structs.keys
  }

  private lazy val allServices: Set[String] = templefile.services.keys.toSet

  private lazy val allStructsAndServices: Set[String] = allServices ++ allStructs

  private def validateAttributes(attributes: Map[String, Attribute])(context: SemanticContext): Unit = {
    attributes.keys.foreach { attributeName =>
      if (attributeName.headOption.forall(!_.isLower))
        context.fail(s"Invalid attribute name $attributeName, it must start with a lowercase letter,")
    }
    attributes.foreachEntry(context(validateAttribute))
  }

  private def validateService(service: ServiceBlock)(context: SemanticContext): Unit = {
    service.structs.foreachEntry(context(validateStruct))
    validateAttributes(service.attributes)(context)
    validateMetadata(service.metadata, service.attributes)(context)
  }

  private def validateStruct(struct: StructBlock)(context: SemanticContext): Unit = {
    struct.attributes.foreachEntry(context(validateAttribute))
    validateMetadata(struct.metadata, struct.attributes)(context)
  }

  private def validateMetadata(metadata: Seq[Metadata], attributes: Map[String, Attribute] = Map())(
    context: SemanticContext,
  ): Unit = {
    def assertUnique[T <: Metadata: ClassTag](): Unit =
      if (metadata.collect { case m: T => m }.sizeIs > 1)
        context.fail(s"Multiple occurrences of ${classTag[T].runtimeClass.getSimpleName} metadata")

    metadata foreach {
      case _: Metadata.TargetLanguage  => assertUnique[Metadata.TargetLanguage]()
      case Metadata.TargetAuth(_)      => context.fail(s"TODO: figure out how this auth works")
      case _: Metadata.ServiceLanguage => assertUnique[Metadata.ServiceLanguage]()
      case _: Metadata.Database        => assertUnique[Metadata.Database]()
      case _: Metadata.ServiceAuth     => assertUnique[Metadata.ServiceAuth]()
      case _: Metadata.Readable        => assertUnique[Metadata.Readable]()
      case Metadata.Writable.All if metadata contains Metadata.Readable.This =>
        context.fail(s"#writable(all) is not compatible with #readable(this)")
      case _: Metadata.Writable          => assertUnique[Metadata.Writable]()
      case Metadata.Omit(_)              => assertUnique[Metadata.Omit]()
      case Metadata.ServiceEnumerable(_) => assertUnique[Metadata.ServiceEnumerable]()
      case _: Metadata.Provider          => assertUnique[Metadata.Provider]()
      case Metadata.Uses(services) =>
        assertUnique[Metadata.Uses]()
        for (service <- services if !allServices.contains(service))
          context.fail(s"No such service $service referenced in #uses")
    }
  }

  private def validateAttribute(attribute: Attribute)(context: SemanticContext): Unit = {
    validateAttributeType(attribute.attributeType)(context)
    attribute.accessAnnotation match {
      case Some(Annotation.Client)    => // nothing to validate
      case Some(Annotation.Server)    => // nothing to validate
      case Some(Annotation.ServerSet) => // nothing to validate
      case None                       => // nothing to validate
    }
    attribute.valueAnnotations foreach {
      case Annotation.Unique   => // nothing to validate
      case Annotation.Nullable => // nothing to validate
    }
  }

  private def validateAttributeType(attributeType: AttributeType)(context: SemanticContext): Unit =
    attributeType match {
      case ForeignKey(references) if !allStructsAndServices.contains(references) =>
        context.fail(s"Invalid foreign key $references")
      case ForeignKey(_) => // all good

      case UUIDType                                      => context.fail(s"Illegal use of UUID type")
      case BoolType | DateType | DateTimeType | TimeType => // nothing to check

      case BlobType(Some(size)) if size < 0 => context.fail(s"BlobType size is negative")
      case BlobType(_)                      => // all good

      case StringType(Some(max), _) if max < 0           => context.fail(s"StringType max is negative")
      case StringType(Some(max), Some(min)) if max < min => context.fail(s"StringType max not above min")
      case StringType(_, _)                              => // all good

      case IntType(Some(max), Some(min), _) if max < min => context.fail(s"IntType max not above min")
      case IntType(_, _, precision) if precision <= 0 || precision > 8 =>
        context.fail(s"IntType precision not between 1 and 8")
      case IntType(_, _, _) => // all good

      case FloatType(Some(max), Some(min), _) if max < min => context.fail(s"FloatType max not above min")
      case FloatType(_, _, precision) if precision <= 0 || precision > 8 =>
        context.fail(s"FloatType precision not between 1 and 8")
      case FloatType(_, _, _) => // all good
    }

  private def validateBlockOfMetadata[T <: Metadata](target: TempleBlock[T])(context: SemanticContext): Unit =
    validateMetadata(target.metadata)(context)

  def validate(): Unit = {
    val context = SemanticContext.empty

    templefile.services.foreachEntry(context(validateService))
    templefile.targets.foreachEntry(context(validateBlockOfMetadata))
    validateBlockOfMetadata(templefile.projectBlock)(context :+ s"${templefile.projectName} project")

    val rootNames =
      allServices.toSeq.map(_       -> "service") ++
      allStructs.map(_              -> "struct") ++
      templefile.targets.keys.map(_ -> "target") :+
      (templefile.projectName -> "project")
    val duplicates = rootNames
      .groupBy(_._1)
      .collect { case (name, repeats) if repeats.sizeIs > 1 => (name, repeats.map(_._2)) }

    if (duplicates.nonEmpty) {
      val duplicateString = duplicates.map { case (name, used) => s"$name (${used.mkString(", ")})" }.mkString(", ")

      val suffix =
        if (duplicates.sizeIs == 1) s"duplicate found: $duplicateString"
        else s"duplicates found: $duplicateString"
      context.fail(s"Project, targets and structs must be globally unique, $suffix")
    }
    rootNames.collect {
      case (name, location) if !name.head.isUpper =>
        context.fail(s"Invalid name: $name ($location), it should start with a capital letter")
    }
  }
}

object Validator {

  def validate(templefile: Templefile): Templefile = {
    val validator = new Validator(templefile)
    validator.validate()
    templefile
  }
}
