package temple.DSL.semantics

import temple.DSL.semantics.NameClashes._
import temple.DSL.semantics.Validator._
import temple.ast.AttributeType._
import temple.ast.{Metadata, _}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

private class Validator(templefile: Templefile) {
  var errors: mutable.Set[String] = mutable.SortedSet()

  private val allStructs: Iterable[String] = templefile.services.flatMap {
    case _ -> service => service.structs.keys
  }

  private var newServices = templefile.services

  private val allServices: Set[String] = templefile.services.keys.toSet

  private val allStructsAndServices: Set[String] = allServices ++ allStructs

  private var newServices: Map[String, ServiceBlock] = templefile.services

  private val globalRenaming      = mutable.Map[String, String]()
  def allGlobalNames: Set[String] = globalRenaming.valuesIterator.toSet ++ allStructsAndServices

  private def validateAttributes(
    attributes: Map[String, Attribute],
    projectName: String,
    context: SemanticContext,
  ): Map[String, Attribute] = {
    foreachValueWithContext(attributes, context, validateAttribute)

    val newAttributes = mutable.LinkedHashMap[String, Attribute]()
    attributes.foreach {
      case (attributeName, value) =>
        if (attributeName.headOption.exists(!_.isLower))
          errors += context.errorMessage(
            s"Invalid attribute name $attributeName, it must start with a lowercase letter,",
          )
        val newName = dodgeNames(
          attributeName,
          projectName,
          (newAttributes.keys ++ attributes.keys).toSet - attributeName,
          decapitalize = true,
        )(postgresValidator)
        newAttributes += newName -> value
    }
    newAttributes.to(attributes.mapFactory)
  }

  private def renameBlock(name: String, block: TempleBlock[_]): Unit =
    if (block.lookupMetadata[Metadata.Database].forall(_ == Metadata.Database.Postgres)) {
      val newServiceName = dodgeNames(name, templefile.projectName, allGlobalNames - name)(postgresValidator)
      globalRenaming += name -> newServiceName
    }

  private val validateService: EntryTransformer[ServiceBlock] = (serviceName, service, context) => {
    renameBlock(serviceName, service)

    val newStructs    = mapEntryWithContext(service.structs, context, validateStruct)
    val newAttributes = validateAttributes(service.attributes, templefile.projectName, context)
    validateMetadata(service.metadata, context)

    ServiceBlock(newAttributes, service.metadata, newStructs)
  }

  private val validateStruct: EntryTransformer[StructBlock] = (structName, struct, context) => {
    renameBlock(structName, struct)

    val newAttributes = validateAttributes(struct.attributes, templefile.projectName, context)
    validateMetadata(struct.metadata, context)

    StructBlock(newAttributes, struct.metadata)
  }

  private val validateMetadata: ValueValidator[Seq[Metadata]] = (metadata, context) => {
    def assertUnique[T <: Metadata: ClassTag](): Unit =
      if (metadata.collect { case m: T => m }.sizeIs > 1)
        errors += context.errorMessage(s"Multiple occurrences of ${classTag[T].runtimeClass.getSimpleName} metadata")

    metadata foreach {
      case _: Metadata.TargetLanguage  => assertUnique[Metadata.TargetLanguage]()
      case _: Metadata.ServiceLanguage => assertUnique[Metadata.ServiceLanguage]()
      case _: Metadata.Database        => assertUnique[Metadata.Database]()
      case _: Metadata.ServiceAuth     => assertUnique[Metadata.ServiceAuth]()
      case _: Metadata.Readable        => assertUnique[Metadata.Readable]()
      case Metadata.Writable.All if metadata contains Metadata.Readable.This =>
        errors += context.errorMessage(s"#writable(all) is not compatible with #readable(this)")
      case _: Metadata.Writable          => assertUnique[Metadata.Writable]()
      case Metadata.Omit(_)              => assertUnique[Metadata.Omit]()
      case Metadata.ServiceEnumerable(_) => assertUnique[Metadata.ServiceEnumerable]()
      case _: Metadata.Provider          => assertUnique[Metadata.Provider]()
      case Metadata.Uses(services) =>
        assertUnique[Metadata.Uses]()
        for (service <- services if !allServices.contains(service))
          errors += context.errorMessage(s"No such service $service referenced in #uses")
    }
  }

  private val validateAttribute: ValueValidator[Attribute] = (attribute, context) => {
    validateAttributeType(attribute.attributeType, context)
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

  private def validateAttributeType(attributeType: AttributeType, context: SemanticContext): Unit =
    attributeType match {
      case ForeignKey(references) if !allStructsAndServices.contains(references) =>
        errors += context.errorMessage(s"Invalid foreign key $references")
      case ForeignKey(_) => // all good

      case UUIDType                                      => errors += context.errorMessage(s"Illegal use of UUID type")
      case BoolType | DateType | DateTimeType | TimeType => // nothing to check

      case BlobType(Some(size)) if size < 0 => errors += context.errorMessage(s"BlobType size is negative")
      case BlobType(_)                      => // all good

      case StringType(Some(max), _) if max < 0 => errors += context.errorMessage(s"StringType max is negative")
      case StringType(Some(max), Some(min)) if max < min =>
        errors += context.errorMessage(s"StringType max not above min")
      case StringType(_, _) => // all good

      case IntType(Some(max), Some(min), _) if max < min => errors += context.errorMessage(s"IntType max not above min")
      case IntType(_, _, precision) if precision <= 0 || precision > 8 =>
        errors += context.errorMessage(s"IntType precision not between 1 and 8")
      case IntType(_, _, _) => // all good

      case FloatType(Some(max), Some(min), _) if max < min =>
        errors += context.errorMessage(s"FloatType max not above min")
      case FloatType(_, _, precision) if precision <= 0 || precision > 8 =>
        errors += context.errorMessage(s"FloatType precision not between 1 and 8")
      case FloatType(_, _, _) => // all good
    }

  private def validateBlockOfMetadata[T <: Metadata]: ValueValidator[TempleBlock[T]] = (target, context) => {
    validateMetadata(target.metadata, context)
  }

  def validate(): Seq[String] = {
    val context = SemanticContext.empty

    newServices = mapEntryWithContext(templefile.services, context, validateService)
    foreachValueWithContext(templefile.targets, context, validateBlockOfMetadata[Metadata.TargetMetadata])
    validateBlockOfMetadata(templefile.projectBlock, context :+ s"${templefile.projectName} project")

    val rootNames =
      allServices.toSeq.map { _       -> "service" } ++
      allStructs.map { _              -> "struct" } ++
      templefile.targets.keys.map { _ -> "target" } :+
      (templefile.projectName -> "project")
    val duplicates = rootNames
      .groupBy(_._1)
      .collect { case (name, repeats) if repeats.sizeIs > 1 => (name, repeats.map(_._2)) }

    if (duplicates.nonEmpty) {
      val duplicateString = duplicates.map { case (name, used) => s"$name (${used.mkString(", ")})" }.mkString(", ")

      val suffix =
        if (duplicates.sizeIs == 1) s"duplicate found: $duplicateString"
        else s"duplicates found: $duplicateString"
      errors += context.errorMessage(s"Project, targets and structs must be globally unique, $suffix")
    }
    rootNames.collect {
      case (name, location) if !name.head.isUpper =>
        errors += context.errorMessage(s"Invalid name: $name ($location), it should start with a capital letter")
    }

    errors.toSeq
  }

  def transformed: Templefile = ServiceRenamer(globalRenaming.toMap)(templefile.copy(services = newServices))
}

object Validator {

  def validationErrors(templefile: Templefile): Set[String] = {
    val validator = new Validator(templefile)
    validator.validate()
    validator.errors.toSet
  }

  def validate(templefile: Templefile): Templefile = {
    val validator   = new Validator(templefile)
    val parseErrors = validator.validate()
    if (parseErrors.nonEmpty) {
      val errorsWere = if (parseErrors.sizeIs == 1) "An error was" else s"${parseErrors.size} errors were"
      throw new SemanticParsingException(
        s"$errorsWere encountered while validating the Templefile\n${parseErrors.mkString("\n")}",
      )
    }
    validator.transformed
  }

//  private type EntryValidator[T] = (String, T, SemanticContext) => Unit
//
//  private def foreachEntryWithContext[T](
//    map: Map[String, T],
//    context: SemanticContext,
//    validate: EntryValidator[T],
//  ): Unit = map.foreachEntry { case (name, t) => validate(name, t, context :+ name) }

  private type ValueValidator[T] = (T, SemanticContext) => Unit

  private def foreachValueWithContext[T](
    map: Map[String, T],
    context: SemanticContext,
    validate: ValueValidator[T],
  ): Unit = map.foreachEntry((name: String, t: T) => validate(t, context :+ name))

  private type EntryTransformer[T] = (String, T, SemanticContext) => T

  private def mapEntryWithContext[T](
    map: Map[String, T],
    context: SemanticContext,
    transform: EntryTransformer[T],
  ): Map[String, T] = map.transform { case (name, t) => transform(name, t, context :+ name) }

//  private type ValueTransformer[T] = (T, SemanticContext) => T
//
//  private def mapValueWithContext[T](
//    map: Map[String, T],
//    context: SemanticContext,
//    transform: ValueTransformer[T],
//  ): Map[String, T] = map.transform { case (name, t) => transform(t, context :+ name) }
}
