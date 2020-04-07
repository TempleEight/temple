package temple.DSL.semantics

import temple.DSL.semantics.NameClashes._
import temple.ast.AbstractServiceBlock._
import temple.ast.AttributeType._
import temple.ast.{Metadata, _}
import temple.builder.project.ProjectConfig
import temple.utils.MonadUtils.FromEither

import scala.collection.immutable.{ListMap, SortedSet}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

private class Validator private (templefile: Templefile) {
  private var errors: mutable.Set[String] = mutable.Set()

  private val allStructs: Iterable[String] = templefile.services.flatMap {
    case _ -> service => service.structs.keys
  }

  private val allServices: Set[String]           = templefile.services.keys.toSet
  private val allStructsAndServices: Set[String] = allServices ++ allStructs

  // The services, updated with renamed attributes
  private var newServices: Map[String, ServiceBlock] = templefile.services

  // A mapping from every service/struct name in the input Templefile to its new name
  private val globalRenaming      = mutable.Map[String, String]()
  def allGlobalNames: Set[String] = globalRenaming.valuesIterator.toSet ++ allStructsAndServices

  private def validateAttributes(
    attributes: Map[String, Attribute],
    context: SemanticContext,
  ): Map[String, Attribute] = {
    attributes.foreach { case (name, t) => validateAttribute(t, context :+ name) }

    // Keep a set of names that have been used already
    val takenNames: mutable.Set[String] = attributes.keys.to(mutable.Set)

    // Add implicit ID Attribute
    ListMap("id" -> IDAttribute) ++ attributes.map {
      case (attributeName, value) =>
        if (attributeName.headOption.exists(!_.isLower))
          errors += context.errorMessage(
            s"Invalid attribute name $attributeName, it must start with a lowercase letter,",
          )
        val newName = constructUniqueName(
          attributeName,
          templefile.projectName,
          // takenNames includes this attribute’s names, so remove it
          takenNames.toSet - attributeName,
          decapitalize = true,
        )(postgresValidator)

        // Prevent this new name from being used by other attributes
        takenNames += newName

        newName -> value
    }
  }

  /** Given a service block or a struct block, find a valid name for it (taking into account the clashes from all the
    * languages that are generated from the block), and entering it into the map of renamings. Note that an entry is
    * always inserted into this block, even if the same name is kept. */
  private def renameBlock(name: String, block: TempleBlock[_]): Unit = {
    val database = block.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase)
    if (database == Metadata.Database.Postgres) {
      val newServiceName = constructUniqueName(name, templefile.projectName, allGlobalNames - name)(postgresValidator)
      globalRenaming += name -> newServiceName
    }
  }

  private def validateService(serviceName: String, service: ServiceBlock, context: SemanticContext): ServiceBlock = {
    renameBlock(serviceName, service)

    val newStructs = service.structs.transform {
      case (name, struct) => validateStruct(name, struct, context :+ name)
    }
    val newAttributes = validateAttributes(service.attributes, context)
    validateMetadata(service.metadata, context)

    ServiceBlock(newAttributes, service.metadata, newStructs)
  }

  private def validateStruct(structName: String, struct: StructBlock, context: SemanticContext): StructBlock = {
    renameBlock(structName, struct)

    val newAttributes = validateAttributes(struct.attributes, context)
    validateMetadata(struct.metadata, context)

    StructBlock(newAttributes, struct.metadata)
  }

  private def validateMetadata(metadata: Seq[Metadata], context: SemanticContext): Unit = {
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

  private def validateAttribute(attribute: Attribute, context: SemanticContext): Unit = {
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

  private def validateBlockOfMetadata[T <: Metadata](target: TempleBlock[T], context: SemanticContext): Unit =
    validateMetadata(target.metadata, context)

  def validate(): Seq[String] = {
    val context = SemanticContext.empty

    newServices = templefile.services.transform {
      case (name, service) => validateService(name, service, context :+ name)
    }
    templefile.targets.foreach {
      case (name, block) => validateBlockOfMetadata(block, context :+ name)
    }

    validateBlockOfMetadata(templefile.projectBlock, context :+ s"${templefile.projectName} project")

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
      errors += context.errorMessage(s"Project, targets and structs must be globally unique, $suffix")
    }
    rootNames.collect {
      case (name, location) if !name.head.isUpper =>
        errors += context.errorMessage(s"Invalid name: $name ($location), it should start with a capital letter")
    }

    errors.toSeq
  }

  /** Perform the renaming at the struct level, using the renamed services */
  def transformed: Templefile = ServiceRenamer(globalRenaming.toMap)(templefile.copy(services = newServices))
}

object Validator {

  /** Take a Templefile and get a set of errors with it */
  def validationErrors(templefile: Templefile): Set[String] = {
    val validator = new Validator(templefile)
    validator.validate().to(SortedSet)
  }

  /** Take a Templefile and find any errors with it, or return a version valid for use in all the languages it will be
    * deployed to */
  def validateEither(templefile: Templefile): Either[Set[String], Templefile] = {
    val validator   = new Validator(templefile)
    val parseErrors = validator.validate()
    if (parseErrors.nonEmpty) Left(parseErrors.to(SortedSet))
    else Right(validator.transformed)
  }

  /** Take a Templefile and convert it to a version valid for use in all the languages it will be deployed to
    *
    * @throws SemanticParsingException a non-empty set of strings representing parse errors
    */
  def validate(templefile: Templefile): Templefile =
    validateEither(templefile) fromEither { parseErrors =>
      val errorsWere = if (parseErrors.sizeIs == 1) "An error was" else s"${parseErrors.size} errors were"
      throw new SemanticParsingException(
        s"$errorsWere encountered while validating the Templefile\n${parseErrors.mkString("\n")}",
      )
    }
}
