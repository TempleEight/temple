package temple.DSL.semantics

import temple.DSL.semantics.MetadataParser.assertNoParameters
import temple.DSL.syntax
import temple.DSL.syntax.{Arg, Args, DSLRootItem, Entry}
import temple.ast.ArgType._
import temple.ast.Metadata._
import temple.ast._
import temple.utils.MapUtils._

import scala.collection.immutable.ListMap
import scala.collection.mutable

object Analyzer {

  /**
    * Convert a sequence of arguments, some of which are named, into a map from name to value
    * @param specs A signature of a sequence of argument names in order, with an optional default assigned to each
    * @param args The argument list to match up with the signature
    * @param context The path to this place in the tree, for use when reporting errors
    * @return A map of named arguments to their values
    * @throws SemanticParsingException when too many, too few, duplicate or unknown arguments are supplied
    */
  private[semantics] def parseParameters(
    specs: (String, Option[syntax.Arg])*,
  )(args: Args)(implicit context: SemanticContext): ArgMap = {
    // A ListMap is an insertion-ordered immutable map
    val specsMap = specs.to(ListMap)
    val argc     = args.posargs.length

    if (specs.sizeIs < argc)
      context.fail(s"Too many arguments supplied to function (found $argc, expected at most ${specs.length})")

    val map = mutable.HashMap[String, syntax.Arg]()

    // Add the positional arguments to the map
    specsMap.lazyZip(args.posargs) foreach {
      case ((name, _), arg) =>
        map.safeInsert(name -> arg, context.fail(s"Programmer error: duplicate argument name $name in spec?"))
    }

    // Add the keyword arguments to the map
    args.kwargs foreach {
      case (name, arg) =>
        if (!specsMap.contains(name)) context.fail(s"Unknown keyword argument $name with value $arg")
        map.safeInsert(name -> arg, context.fail(s"Duplicate argument provided for $name"))
    }

    // Add the default arguments to the map
    specsMap.iterator.drop(argc) foreach {
      case (name, default) =>
        // Ensure each key now exists in the argument map. If it does not, fall back to the default value.
        // If there is no default value, throw an error.
        map.getOrElseUpdate(
          name,
          default.getOrElse { context.fail(s"Required argument $name not provided") },
        )
    }

    ArgMap(map.toMap)
  }

  /**
    * Parse the type of an attribute from a raw AST to a meaningful data type
    * @param dataType The raw AST entry, consisting of a type and optionally some arguments (both positional and named):
    *                 `bool`, `int(max: 100)`, `string(20)`.
    * @param context Where this attribute is tagged
    * @return A parsed attribute type
    */
  private def parseAttributeType(dataType: syntax.AttributeType)(implicit context: SemanticContext): AttributeType = {
    implicit val innerContext: SemanticContext = context :+ dataType.typeName
    dataType match {
      case syntax.AttributeType.Foreign(typeName) => AttributeType.ForeignKey(typeName)
      case syntax.AttributeType.Primitive(typeName, args) =>
        typeName match {
          case "int" =>
            val argMap = parseParameters(
              "max"       -> Some(Arg.NoArg),
              "min"       -> Some(Arg.NoArg),
              "precision" -> Some(Arg.IntArg(4)),
            )(args)(innerContext)
            AttributeType.IntType(
              argMap.getOptionArg("min", IntArgType)(innerContext),
              argMap.getOptionArg("max", IntArgType)(innerContext),
              argMap.getArg("precision", IntArgType)(innerContext).toByte,
            )
          case "string" =>
            val argMap = parseParameters(
              "maxLength" -> Some(Arg.NoArg),
              "minLength" -> Some(Arg.NoArg),
            )(args)(innerContext)
            AttributeType.StringType(
              argMap.getOptionArg("maxLength", IntArgType)(innerContext),
              argMap.getOptionArg("minLength", IntArgType)(innerContext).map(_.toInt),
            )
          case "float" =>
            val argMap = parseParameters(
              "max"       -> Some(Arg.NoArg),
              "min"       -> Some(Arg.NoArg),
              "precision" -> Some(Arg.IntArg(8)),
            )(args)(innerContext)
            AttributeType.FloatType(
              argMap.getOptionArg("max", FloatingArgType)(innerContext),
              argMap.getOptionArg("min", FloatingArgType)(innerContext),
              argMap.getArg("precision", FloatingArgType)(innerContext).toByte,
            )
          case "data" =>
            val argMap = parseParameters("maxSize" -> Some(Arg.NoArg))(args)(innerContext)
            AttributeType.BlobType(
              argMap.getOptionArg("maxSize", IntArgType)(innerContext),
            )
          case "date" =>
            assertNoParameters(args)(innerContext)
            AttributeType.DateType
          case "datetime" =>
            assertNoParameters(args)(innerContext)
            AttributeType.DateTimeType
          case "time" =>
            assertNoParameters(args)(innerContext)
            AttributeType.TimeType
          case "bool" =>
            assertNoParameters(args)(innerContext)
            AttributeType.BoolType
          case typeName => context.fail(s"Unknown type $typeName")
        }
    }
  }

  private def parseAttribute(
    dataType: syntax.AttributeType,
    annotations: Seq[syntax.Annotation],
  )(implicit context: SemanticContext): Attribute = {
    var accessAnnotation: Option[Annotation.AccessAnnotation] = None

    def setAccessAnnotation(annotation: Annotation.AccessAnnotation): Unit =
      accessAnnotation.fold { accessAnnotation = Some(annotation) } { existingAnnotation =>
        context.fail(
          s"Two scope annotations found: ${annotation.render} is incompatible with ${existingAnnotation.render}",
        )
      }

    val valueAnnotations = mutable.HashSet[Annotation.ValueAnnotation]()
    annotations.iterator.map(_.key) foreach {
      case "unique"    => valueAnnotations += Annotation.Unique
      case "nullable"  => valueAnnotations += Annotation.Nullable
      case "server"    => setAccessAnnotation(Annotation.Server)
      case "client"    => setAccessAnnotation(Annotation.Client)
      case "serverSet" => setAccessAnnotation(Annotation.ServerSet)
      case key         => context.fail(s"Unknown annotation @$key")
    }
    Attribute(parseAttributeType(dataType), accessAnnotation, valueAnnotations.toSet)
  }

  /** A parser of Metadata items that can occur in service blocks */
  private val parseServiceMetadata = new MetadataParser[ServiceMetadata] {
    registerKeywordWithContext("language", TokenArgType)(ServiceLanguage)
    registerKeywordWithContext("database", TokenArgType)(Database)
    registerEmptyKeyword("enumerable")(ServiceEnumerable())
    registerEmptyKeyword("enumerableByThis")(ServiceEnumerable(byThis = true))
    registerKeywordWithContext("readable", "by", TokenArgType)(Readable)
    registerKeywordWithContext("writable", "by", TokenArgType)(Writable)
    registerKeywordWithContext("auth", "type", TokenArgType)(ServiceAuth)
    registerKeyword("uses", "services", ListArgType(TokenArgType))(Uses)
    registerKeywordWithContext("omit", "endpoints", ListArgType(TokenArgType))(Omit)
  }

  /** A parser of Metadata items that can occur in struct blocks */
  private val parseStructMetadata = new MetadataParser[StructMetadata] {
    registerEmptyKeyword("enumerable")(ServiceEnumerable())
    registerEmptyKeyword("enumerableByThis")(ServiceEnumerable(byThis = true))
    registerKeywordWithContext("readable", "by", TokenArgType)(Readable)
    registerKeywordWithContext("writable", "by", TokenArgType)(Writable)
    registerKeywordWithContext("omit", "endpoints", ListArgType(TokenArgType))(Omit)
  }

  /** A parser of Metadata items that can occur in project blocks */
  private val parseProjectMetadata = new MetadataParser[ProjectMetadata] {
    registerKeywordWithContext("language", TokenArgType)(ServiceLanguage)
    registerKeywordWithContext("database", TokenArgType)(Database)
    registerKeywordWithContext("provider", TokenArgType)(Provider)
    registerKeywordWithContext("readable", "by", TokenArgType)(Readable)
    registerKeywordWithContext("writable", "by", TokenArgType)(Writable)
  }

  /** A parser of Metadata items that can occur in target blocks */
  private val parseTargetMetadata = new MetadataParser[TargetMetadata] {
    registerKeywordWithContext("language", TokenArgType)(TargetLanguage)
  }

  /**
    * Parse a service block from a list of entries into the distinct attributes, metadatas and structs
    *
    * @param entries The list of entries in the block from the AST
    * @param context The location in the AST, used for error messages
    * @return A semantic representation of a [[temple.ast.ServiceBlock]]
    */
  private def parseServiceBlock(entries: Seq[Entry])(implicit context: SemanticContext): ServiceBlock = {
    // LinkedHashMap is used to preserve order in the map
    val attributes = mutable.LinkedHashMap[String, Attribute]()
    val metadatas  = mutable.ListBuffer[ServiceMetadata]()
    val structs    = mutable.LinkedHashMap[String, StructBlock]()
    entries.foreach {
      case Entry.Attribute(key, dataType, annotations) =>
        attributes.safeInsert(key -> parseAttribute(dataType, annotations)(context :+ key))
      case Entry.Metadata(metaKey, args) => metadatas += parseServiceMetadata(metaKey, args)
      case DSLRootItem(key, tag, entries) =>
        tag match {
          case "struct" => structs.safeInsert(key -> parseStructBlock(entries)(context :+ s"$key $tag"))
          case tag      => context.fail(s"Unknown block type $tag for $key")
        }
    }
    ServiceBlock(attributes.to(ListMap), metadatas.toSeq, structs.to(ListMap))
  }

  /** Parse a block consisting solely of metadata */
  private def parseMetadataBlock[T <: Metadata](
    entries: Seq[Entry],
    f: MetadataParser[T],
  )(implicit context: SemanticContext): Seq[T] = parseBlockWithMetadata(entries, f)()

  /** Parse a block containing metadata, among other things */
  private def parseBlockWithMetadata[T <: Metadata](entries: Seq[Entry], f: MetadataParser[T])(
    parser: PartialFunction[Entry, Unit] = PartialFunction.empty,
  )(implicit context: SemanticContext): Seq[T] = entries flatMap { entry =>
    // When the custom handlers are performed, succeed with no result
    val customParser = parser.andThen(_ => None)

    // When metadata is encountered, succeed with the result of passing it to `f`
    val metadataParser: PartialFunction[Entry, Option[T]] = {
      case Entry.Metadata(metaKey, args) => Some(f(metaKey, args))
    }

    // Combine the custom and metadata parsers
    val combinedParser = customParser.orElse(metadataParser)

    // Pass the entry to either the combined parser, or fail with a relevant message
    combinedParser.lift(entry).getOrElse {
      context.fail(s"Found unexpected ${entry.typeName}: `$entry`")
    }
  }

  private def parseProjectBlock(entries: Seq[Entry])(implicit context: SemanticContext): ProjectBlock =
    ProjectBlock(parseMetadataBlock(entries, parseProjectMetadata))

  private def parseTargetBlock(entries: Seq[Entry])(implicit context: SemanticContext): TargetBlock =
    TargetBlock(parseMetadataBlock(entries, parseTargetMetadata))

  private def parseStructBlock(entries: Seq[Entry])(implicit context: SemanticContext): StructBlock = {
    val attributes = mutable.LinkedHashMap[String, Attribute]()
    val metadata = parseBlockWithMetadata(entries, parseStructMetadata) {
      case Entry.Attribute(key, dataType, annotations) =>
        attributes.safeInsert(key -> parseAttribute(dataType, annotations)(context :+ key))
    }

    StructBlock(attributes.toMap, metadata)
  }

  /**
    * Turns an AST of a Templefile into a semantic representation
    * @param templefile the AST parsed from the Templefile
    * @return A semantic representation of the project, as well as all the targets and services, defined in the
    *         Templefile
    * @throws SemanticParsingException when there is no project information, as well as when any of the definitions are
    *                                  malformed
    */
  private[semantics] def parseSemantics(templefile: syntax.Templefile): Templefile = {
    var projectNameBlock: Option[(String, ProjectBlock)] = None

    val targets  = mutable.LinkedHashMap[String, TargetBlock]()
    val services = mutable.LinkedHashMap[String, ServiceBlock]()

    templefile.foreach {
      case DSLRootItem(key, tag, entries) =>
        implicit val context: SemanticContext = SemanticContext.empty :+ s"$key $tag"
        tag match {
          // TODO: error message
          case "service" => services.safeInsert(key -> parseServiceBlock(entries))
          case "project" =>
            projectNameBlock.fold { projectNameBlock = Some(key -> parseProjectBlock(entries)) } {
              case (str, _) => context.fail(s"Second project found in addition to $str,")
            }
          // TODO: error message
          case "target" => targets.safeInsert(key -> parseTargetBlock(entries))

          case _ => context.fail(s"Unknown block type")
        }
    }

    val (projectName, projectBlock) = projectNameBlock.getOrElse {
      throw new SemanticParsingException("Temple file has no project block")
    }

    Templefile(projectName, projectBlock, targets.to(ListMap), services.to(ListMap))
  }

  /**
    * Turns an AST of a Templefile into a semantic representation and validates it
    * @param templefileAST the AST parsed from the Templefile
    * @return A semantic representation of the project, as well as all the targets and services, defined in the
    *         Templefile
    * @throws SemanticParsingException when there is no project information, as well as when any of the definitions are
    *                                  malformed or the contents is not valid
    */
  def parseAndValidate(templefileAST: syntax.Templefile): Templefile = Validator.validate(parseSemantics(templefileAST))
}
