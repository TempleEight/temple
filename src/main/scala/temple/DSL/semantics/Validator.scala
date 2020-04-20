package temple.DSL.semantics

import temple.DSL.semantics.NameClashes._
import temple.ast.AbstractAttribute.{Attribute, CreatedByAttribute, IDAttribute}
import temple.ast.AbstractServiceBlock._
import temple.ast.AttributeType._
import temple.ast.Metadata.ServiceAuth
import temple.ast._
import temple.builder.project.ProjectConfig
import temple.utils.MonadUtils.FromEither

import scala.Option.when
import scala.collection.immutable.{ListMap, SortedSet}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

private class Validator private (templefile: Templefile) {
  private var errors: mutable.Set[String] = mutable.Set()

  // The services, updated with renamed attributes
  private var newServices: Map[String, ServiceBlock] = templefile.services

  // A mapping from every service/struct name in the input Templefile to its new name
  private val globalRenaming      = mutable.Map[String, String]()
  def allGlobalNames: Set[String] = globalRenaming.valuesIterator.toSet ++ templefile.providedBlockNames

  private def validateAttributes(
    block: AttributeBlock[_],
    context: SemanticContext,
  ): Map[String, AbstractAttribute] = {
    block.attributes.foreach { case (name, t) => validateAttribute(t, context :+ name) }

    // Keep a set of names that have been used already
    val takenNames: mutable.Set[String] = block.attributes.keys.to(mutable.Set)

    val usesCreatedBy: Boolean = templefile.usesAuth && !block.hasMetadata(Metadata.ServiceAuth)

    val implicitAttributes: ListMap[String, AbstractAttribute] =
      (ListMap("id" -> IDAttribute)
      ++ when(usesCreatedBy) { "createdBy" -> CreatedByAttribute })

    // Add implicit attributes
    implicitAttributes ++ block.attributes.map {
      case (attributeName, value) =>
        if (attributeName.headOption.exists(!_.isLower))
          errors += context.errorMessage(
            s"Invalid attribute name $attributeName, it must start with a lowercase letter,",
          )
        val database = block.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase)
        val language = block.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)

        // takenNames includes this attribute’s names, so remove it
        val newName = constructUniqueName(
          attributeName,
          templefile.projectName,
          takenNames.toSet - attributeName,
        )(
          validators = Seq(
            Some(templeAttributeNameValidator),
            when(database == Metadata.Database.Postgres) { postgresValidator },
            when(language == Metadata.ServiceLanguage.Go) { goAttributeValidator },
          ).flatten: _*,
        )

        // Prevent this new name from being used by other attributes
        takenNames += newName

        newName -> value
    }
  }

  /** Given a service block or a struct block, find a valid name for it (taking into account the clashes from all the
    * languages that are generated from the block), and entering it into the map of renamings. Note that an entry is
    * always inserted into this block, even if the same name is kept. */
  private def renameBlock(name: String, block: AttributeBlock[_]): Unit = {
    val database = block.lookupMetadata[Metadata.Database].getOrElse(ProjectConfig.defaultDatabase)
    val language = block.lookupMetadata[Metadata.ServiceLanguage].getOrElse(ProjectConfig.defaultLanguage)
    val newServiceName = constructUniqueName(name, templefile.projectName, allGlobalNames - name)(
      validators = Seq(
        Some(templeServiceNameValidator),
        when(database == Metadata.Database.Postgres) { postgresValidator },
        when(language == Metadata.ServiceLanguage.Go) { goServiceValidator },
      ).flatten: _*,
    )
    globalRenaming += name -> newServiceName
  }

  private def validateService(serviceName: String, service: ServiceBlock, context: SemanticContext): ServiceBlock = {
    renameBlock(serviceName, service)

    val newStructs = service.structs.transform {
      case (name, struct) => validateStruct(name, struct, context :+ name)
    }
    val newAttributes = validateAttributes(service, context)
    val newMetadata   = validateStructMetadata(service.metadata, newAttributes, context)

    ServiceBlock(newAttributes, newMetadata, newStructs)
  }

  private def validateStruct(
    structName: String,
    struct: StructBlock,
    context: SemanticContext,
  ): StructBlock = {
    renameBlock(structName, struct)

    val newAttributes = validateAttributes(struct, context)
    val newMetadata   = validateStructMetadata(struct.metadata, newAttributes, context)

    StructBlock(newAttributes, newMetadata)
  }

  private def validateStructMetadata[M >: Metadata.StructMetadata <: Metadata](
    metadata: Seq[M],
    attributes: Map[String, AbstractAttribute],
    context: SemanticContext,
  ): Seq[M] = {
    validateMetadata(metadata, context)

    val removeUpdateEndpoint = attributes.valuesIterator.forall(!_.inRequest)
    if (removeUpdateEndpoint) {
      val (endpoints, otherMetadata) = metadata partitionMap {
          case Metadata.Omit(endpoints) => Left(endpoints)
          case m                        => Right(m)
        }
      otherMetadata :+ Metadata.Omit(endpoints.flatten.toSet + Metadata.Endpoint.Update)
    } else metadata
  }

  private def validateMetadata(metadata: Seq[Metadata], context: SemanticContext): Unit = {
    def assertUnique[T <: Metadata: ClassTag](): Unit =
      if (metadata.collect { case m: T => m }.sizeIs > 1)
        errors += context.errorMessage(s"Multiple occurrences of ${classTag[T].runtimeClass.getSimpleName} metadata")

    metadata foreach {
      case _: Metadata.TargetLanguage  => assertUnique[Metadata.TargetLanguage]()
      case _: Metadata.ServiceLanguage => assertUnique[Metadata.ServiceLanguage]()
      case _: Metadata.Database        => assertUnique[Metadata.Database]()
      case _: Metadata.AuthMethod =>
        if (templefile.services.valuesIterator.forall { _.lookupLocalMetadata[ServiceAuth].isEmpty }) {
          errors += context.errorMessage(s"#authMethod requires at least one block to have #auth declared")
        }
        assertUnique[Metadata.AuthMethod]()
      case _: Metadata.ServiceAuth if !templefile.usesAuth =>
        errors += context.errorMessage(s"#auth requires an #authMethod to be declared for the project")
      case _: Metadata.ServiceAuth => assertUnique[Metadata.ServiceAuth]()
      case Metadata.Readable.This if !templefile.usesAuth =>
        errors += context.errorMessage(s"#readable(this) requires an #authMethod to be declared for the project")
      case _: Metadata.Readable => assertUnique[Metadata.Readable]()
      case Metadata.Writable.All if metadata contains Metadata.Readable.This =>
        errors += context.errorMessage(s"#writable(all) is not compatible with #readable(this)")
      case Metadata.Writable.This if !templefile.usesAuth =>
        errors += context.errorMessage(s"#writable(this) requires an #authMethod to be declared for the project")
      case _: Metadata.Writable       => assertUnique[Metadata.Writable]()
      case Metadata.Omit(_)           => assertUnique[Metadata.Omit]()
      case Metadata.ServiceEnumerable => assertUnique[Metadata.ServiceEnumerable]()
      case _: Metadata.Provider       => assertUnique[Metadata.Provider]()
      case _: Metadata.Metrics        => assertUnique[Metadata.Metrics]()
      case Metadata.Uses(services) =>
        assertUnique[Metadata.Uses]()
        for (service <- services if !templefile.services.contains(service))
          errors += context.errorMessage(s"No such service $service referenced in #uses")
    }
  }

  private def validateAttribute(attribute: AbstractAttribute, context: SemanticContext): Unit = {
    validateAttributeType(attribute.attributeType, context)
    attribute.accessAnnotation match {
      case Some(Annotation.Client)    => // nothing to validate
      case Some(Annotation.Server)    => // nothing to validate
      case Some(Annotation.ServerSet) => // nothing to validate
      case None                       => // nothing to validate
    }
    attribute.valueAnnotations foreach {
      case Annotation.Unique => // nothing to validate
    }
  }

  private def validateAttributeType(attributeType: AttributeType, context: SemanticContext): Unit =
    attributeType match {
      case ForeignKey(references) if !templefile.providedBlockNames.contains(references) =>
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
      case intType: IntType if intType.minValue > intType.precisionMax =>
        errors += context.errorMessage(s"IntType min is out of range for the precision ${intType.precision}")
      case intType: IntType if intType.maxValue < intType.precisionMin =>
        errors += context.errorMessage(s"IntType max is out of range for the precision ${intType.precision}")
      case IntType(_, _, _) => // all good

      case FloatType(Some(max), Some(min), _) if max < min =>
        errors += context.errorMessage(s"FloatType max not above min")
      case FloatType(_, _, precision) if precision <= 0 || precision > 8 =>
        errors += context.errorMessage(s"FloatType precision not between 1 and 8")
      case FloatType(_, _, _) => // all good
    }

  private def validateBlockOfMetadata[T <: Metadata](target: TempleBlock[T], context: SemanticContext): Unit =
    validateMetadata(target.metadata, context)

  private val referenceCycles: Set[Set[String]] = {
    val graph = templefile.providedBlockNames.map { blockName =>
      blockName -> templefile.getBlock(blockName).attributes.values.collect {
        case Attribute(ForeignKey(references), _, _) => references
      }
    }.toMap
    Tarjan(graph)
  }

  def validate(): Seq[String] = {
    val context = SemanticContext.empty

    newServices = templefile.services.transform {
      case (name, service) => validateService(name, service, context :+ name)
    }
    templefile.targets.foreach {
      case (name, block) => validateBlockOfMetadata(block, context :+ name)
    }

    validateBlockOfMetadata(templefile.projectBlock, context :+ s"${templefile.projectName} project")

    val rootNames: Seq[(String, String)] =
      Seq(templefile.projectName     -> "project") ++
      templefile.services.keys.map(_ -> "service") ++
      templefile.structNames.map(_   -> "struct") ++
      templefile.targets.keys.map(_  -> "target")
    val duplicates = rootNames
      .groupBy(_._1)
      .collect { case (name, repeats) if repeats.sizeIs > 1 => (name, repeats.map(_._2)) }

    if (duplicates.nonEmpty) {
      val duplicateString =
        duplicates.map { case (name, used) => s"$name (${used.sorted.mkString(", ")})" }.toSeq.sorted.mkString(", ")

      val suffix =
        if (duplicates.sizeIs == 1) s"duplicate found: $duplicateString"
        else s"duplicates found: $duplicateString"
      errors += context.errorMessage(s"Project, targets and structs must be globally unique, $suffix")
    }
    rootNames.collect {
      case (name, location) if !name.head.isUpper =>
        errors += context.errorMessage(s"Invalid name: $name ($location), it should start with a capital letter")
    }

    if (referenceCycles.nonEmpty) {
      val referenceCycleStrings = referenceCycles.map(_.toSeq.sorted.mkString("{ ", ", ", " }"))
      errors += ("Cycle(s) were detected in foreign keys, between elements: " + referenceCycleStrings.mkString(", "))
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

  /**
    * Take a Templefile and convert it to a version valid for use in all the languages it will be deployed to
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
