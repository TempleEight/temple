package temple.DSL.semantics

import temple.DSL.semantics.ArgType._
import temple.DSL.semantics.Metadata._
import temple.DSL.semantics.Templefile._
import temple.DSL.Syntax
import temple.DSL.Syntax.{Arg, Args, DSLRootItem, Entry}
import temple.utils.MapUtils._

import scala.collection.immutable.ListMap
import scala.collection.mutable

object Analyser {

  /**
    * Throws an exception about the semantic analysis.
    * @param str A string representation of the error
    * @return never returns
    */
  // TODO: better error handling
  //   When parsing into an AST, we could pass around a mutable context, which we use to log the location of each part
  //   of the code. This could also be a good way of enforcing that each name is unique (at the moment there is no
  //   checking that for example the names of structs and fields are distinct). A mutable Trie or HashMap would work
  //   well for this
  // TODO: This function could take the context as an implicit argument.
  private def fail(str: String): Nothing        = throw new SemanticParsingException(str)
  implicit private val failHandler: FailHandler = fail

  // TODO: add more path
  private case class KeyName(keyName: String)  { override def toString: String = keyName  }
  private case class Context(function: String) { override def toString: String = function }

  private case class BlockContext private (block: String, tag: String, context: Option[BlockContext]) {

    override def toString: String = context.fold(s"$tag ($block)") { context =>
      s"$tag ($block, inside $context)"
    }
  }

  private object BlockContext {
    def apply(block: String, tag: String, wrapper: BlockContext): BlockContext = BlockContext(block, tag, Some(wrapper))
    def apply(block: String, tag: String): BlockContext                        = BlockContext(block, tag, None)
  }

  /**
    * Convert a sequence of arguments, some of which are named, into a map from name to value
    * @param specs A signature of a sequence of argument names in order, with an optional default assigned to each
    * @param args The argument list to match up with the signature
    * @param context The path to this place in the tree, for use when reporting errors
    * @return A map of named arguments to their values
    * @throws SemanticParsingException when too many, too few, duplicate or unknown arguments are supplied
    */
  private def parseParameters(specs: (String, Option[Syntax.Arg])*)(args: Args)(implicit context: Context): ArgMap = {
    // A ListMap is an insertion-ordered immutable map
    val specsMap = specs.to(ListMap)
    val argc     = args.posargs.length

    if (specs.sizeIs < argc)
      fail(s"Too many arguments supplied to function $context (found $argc, expected at most ${specs.length})")

    val map = mutable.HashMap[String, Syntax.Arg]()

    // Add the positional arguments to the map
    specsMap.lazyZip(args.posargs) foreach {
      case ((name, _), arg) =>
        map.safeInsert(name -> arg, fail(s"Programmer error: duplicate argument name $name in spec for $context?"))
    }

    // Add the keyword arguments to the map
    args.kwargs foreach {
      case (name, arg) =>
        if (!specsMap.contains(name)) fail(s"Unknown keyword argument $name with value $arg for $context")
        map.safeInsert(name -> arg, fail(s"Duplicate argument provided for $name for $context"))
    }

    // Add the default arguments to the map
    specsMap.iterator.drop(argc) foreach {
      case (name, default) =>
        // Ensure each key now exists in the argument map. If it does not, fall back to the default value.
        // If there is no default value, throw an error.
        map.getOrElseUpdate(
          name,
          default.getOrElse { fail(s"Required argument $name not provided for $context") }
        )
    }

    ArgMap(map.toMap)
  }

  /**
    * A wrapper around a map of arguments, as produced by [[temple.DSL.semantics#parseParameters]], with methods added
    * to extract arguments of given types
    *
    * @param argMap The underlying immutable map of names to argument values
    */
  private case class ArgMap(argMap: Map[String, Arg]) {

    /**
      * Type-safely extract an argument from the argument map
      * @param key The name of the argument to extract
      * @param argType The type of the argument to extract
      * @param context The location of the function call, used in the error message
      * @tparam T The type of the element to extract
      * @return The typesafe extracted value
      */
    def getArg[T](key: String, argType: ArgType[T])(implicit context: Context): T =
      argType.extractArg(argMap(key)).getOrElse {
        fail(s"${argType.stringRep.capitalize} expected at $key for $context, found ${argMap(key)}")
      }

    /**
      * Type-safely extract [[Some]] argument from the argument map, or [[None]] if the default value is
      * [[temple.DSL.Syntax.Arg.NoArg]]
      * @param key The name of the argument to extract
      * @param argType The type of the argument to extract
      * @param context The location of the function call, used in the error message
      * @tparam T The type of the element to extract
      * @return [[Some]] typesafe extracted value, or [[None]] if it was not provided and the default was
      *         [[temple.DSL.Syntax.Arg.NoArg]]
      */
    def getOptionArg[T](key: String, argType: ArgType[T])(implicit context: Context): Option[T] =
      argMap(key) match { case Arg.NoArg => None; case _ => Some(getArg(key, argType)) }
  }

  // TODO: is this too messy?
  def parseAttributeType(dataType: Syntax.AttributeType)(implicit keyNameContext: KeyName): AttributeType = {
    val Syntax.AttributeType(typeName, args) = dataType
    implicit val context: Context            = Context(s"$typeName@$keyNameContext")

    typeName match {
      case "int" =>
        val argMap = parseParameters(
          "max"       -> Some(Arg.NoArg),
          "min"       -> Some(Arg.NoArg),
          "precision" -> Some(Arg.IntArg(4))
        )(args)
        AttributeType.IntType(
          argMap.getOptionArg("min", IntArgType),
          argMap.getOptionArg("max", IntArgType),
          argMap.getArg("precision", IntArgType).toShort
        )
      case "string" =>
        val argMap = parseParameters(
          "maxLength" -> Some(Arg.NoArg),
          "minLength" -> Some(Arg.IntArg(0))
        )(args)
        AttributeType.StringType(
          argMap.getOptionArg("maxLength", IntArgType),
          argMap.getArg("minLength", IntArgType).toInt
        )
      case "float" =>
        val argMap = parseParameters(
          "max"       -> Some(Arg.FloatingArg(Double.MaxValue)),
          "min"       -> Some(Arg.FloatingArg(Double.MinValue)),
          "precision" -> Some(Arg.IntArg(8))
        )(args)
        AttributeType.FloatType(
          argMap.getArg("max", FloatingArgType),
          argMap.getArg("min", FloatingArgType),
          argMap.getArg("precision", FloatingArgType).toShort
        )
      case "date"     => AttributeType.DateType
      case "datetime" => AttributeType.DateTimeType
      case "time"     => AttributeType.TimeType
      case "data" =>
        val argMap = parseParameters("maxSize" -> Some(Arg.NoArg))(args)
        AttributeType.BlobType(
          argMap.getOptionArg("maxSize", IntArgType)
        )
      case "bool"   => AttributeType.BoolType
      case typeName => fail(s"Unknown type $typeName")
    }
  }

  def parseAttribute(
    dataType: Syntax.AttributeType,
    annotations: Seq[Syntax.Annotation]
  )(implicit keyNameContext: KeyName): Attribute = {
    var accessAnnotation: Option[Annotation.AccessAnnotation] = None

    def setAccessAnnotation(annotation: Annotation.AccessAnnotation): Unit =
      accessAnnotation.fold { accessAnnotation = Some(annotation) } { existingAnnotation =>
        fail(
          s"Two scope annotations found for ${keyNameContext.keyName}: " +
          s"${annotation.render} is incompatible with ${existingAnnotation.render}"
        )
      }
    val valueAnnotations = mutable.HashSet[Annotation.ValueAnnotation]()
    annotations.iterator.map(_.key) foreach {
      case "unique"    => valueAnnotations += Annotation.Unique
      case "server"    => setAccessAnnotation(Annotation.Server)
      case "client"    => setAccessAnnotation(Annotation.Client)
      case "serverSet" => setAccessAnnotation(Annotation.ServerSet)
      case key         => fail(s"Unknown annotation @$key at ${keyNameContext.keyName}")
    }
    Attribute(parseAttributeType(dataType), accessAnnotation, valueAnnotations.toSet)
  }

  /**
    * Build a metadata parser by building a list of function calls for each match
    *
    * This is needed because each call to [[temple.DSL.semantics.Analyser.MetadataParser#registerKeyword]] is effectively
    * dependently typed, so it cannot be encoded into a tuple, but instead must immediately be bundled into a function
    *
    * @tparam T the subtype of metadata that we are specifically parsing. The “inheritance” (built up with
    *           `inherit from myMetadataParser`) runs in the opposite direction to the inheritance of classes: each
    *           parser extends parsers of a more specific type.
    */
  class MetadataParser[T <: Metadata]() {
    private type Matcher = Args => T
    private var matchers = mutable.LinkedHashMap[String, Matcher]()

    /**
      * Use a different parser as a fallback if none of the parsers defined here match
      * @param parser the alternative parser to use as a fallback
      * @tparam A the type of metadata parsed by `parser`, which must be equal to/a subtype of this parser’s metadata
      *           type
      */
    def inheritFrom[A <: T](parser: MetadataParser[A]): Unit =
      // insert, or no-op if they already exist as inheritance does not overwrite newly added values
      parser.matchers.foreach(matchers.safeInsert(_, ()))

    /** Used for a fluent API style: `inherit from myMetadataParser` as an alternative to
      * `inheritFrom(myMetadataParser)` */
    final protected object inherit {

      /** An alias of [[MetadataParser#inheritFrom]], for a fluent API */
      def from[A <: T](parser: MetadataParser[A]): Unit = inheritFrom(parser)
    }

    /**
      * Add a handler for a new type of metadata
      *
      * @param metaKey The name of the metadata item to add
      * @param argKey The name of the single argument to the metadata. Note that there is also
      *               [[temple.DSL.semantics.Analyser.MetadataParser#registerKeyword]]
      * @param argType The type of the field to expect
      * @param constructor The function to turn an input of type [[ArgType]] into a value of type [[T]]
      * @tparam A The underlying type of the field, inferred from `argType`
      */
    // TODO: do we need to add support for multiple arguments in future?
    final protected def registerKeyword[A](
      metaKey: String,
      argKey: String,
      argType: ArgType[A],
      constructor: A => T
    ): Unit =
      matchers += (metaKey -> { args =>
          implicit val context: Context = Context(metaKey)
          val argMap                    = parseParameters(argKey -> None)(args)
          constructor(argMap.getArg(argKey, argType))
        })

    /** A shorthand for [[temple.DSL.semantics.Analyser.MetadataParser#registerKeyword]] with the same `key` used for both the
      * metadata name and its single argument */
    final protected def registerKeyword[A](key: String, argType: ArgType[A], constructor: A => T): Unit =
      registerKeyword(key, key, argType, constructor)

    /** Perform parsing by looking up the relevant function and
      * [[scala.collection.IterableOnceOps#foldRight(java.lang.Object, scala.Function2)]] supports */
    final def apply(metaKey: String, args: Args)(implicit context: BlockContext): T =
      matchers.get(metaKey).map(_(args)) getOrElse {
        fail(s"No valid metadata $metaKey in $context")
      }
  }

  /** A parser of Metadata items that can occur in all the blocks */
  private val parseGlobalMetadata = new MetadataParser[GlobalMetadata] {
    registerKeyword("language", TokenArgType, Language)
  }

  /** A parser of Metadata items that can occur in project and service blocks */
  private val parseProjectAndServiceMetadata = new MetadataParser[ProjectAndServiceMetadata] {
    inherit from parseGlobalMetadata
    registerKeyword("database", TokenArgType, Database)
  }

  /** A parser of Metadata items that can occur in service blocks */
  private val parseServiceMetadata = new MetadataParser[ServiceMetadata] {
    inherit from parseProjectAndServiceMetadata
    registerKeyword("auth", "login", TokenArgType, ServiceAuth)
    registerKeyword("uses", "services", ListArgType(TokenArgType), Uses)
  }

  /** A parser of Metadata items that can occur in project blocks */
  private val parseProjectMetadata = new MetadataParser[ProjectMetadata] {
    inherit from parseProjectAndServiceMetadata
    registerKeyword("provider", TokenArgType, Provider)
  }

  /** A parser of Metadata items that can occur in target blocks */
  private val parseTargetMetadata = new MetadataParser[TargetMetadata] {
    inherit from parseGlobalMetadata
    registerKeyword("auth", "services", ListArgType(TokenArgType), TargetAuth)
  }

  /**
    * Parse a service block from a list of entries into the distinct attributes, metadatas and structs
    *
    * @param entries The list of entries in the block from the AST
    * @param context The location in the AST, used for error messages
    * @return A semantic representation of a [[temple.DSL.semantics.ServiceBlock]]
    */
  private def parseServiceBlock(entries: Seq[Entry])(implicit context: BlockContext): ServiceBlock = {
    val attributes = mutable.LinkedHashMap[String, Attribute]()
    val metadatas  = mutable.ListBuffer[ServiceMetadata]()
    val structs    = mutable.LinkedHashMap[String, StructBlock]()
    entries.foreach {
      case Entry.Attribute(key, dataType, annotations) =>
        attributes.safeInsert(key -> parseAttribute(dataType, annotations)(KeyName(key)))
      case Entry.Metadata(metaKey, args) => metadatas += parseServiceMetadata(metaKey, args)
      case DSLRootItem(key, tag, entries) =>
        tag match {
          case "struct" => structs.safeInsert(key -> parseStructBlock(entries)(BlockContext(key, tag, context)))
          case tag      => fail(s"Unknown block type $tag for $key in $context")
        }
    }
    ServiceBlock(attributes.to(ListMap), metadatas.toSeq, structs.to(ListMap))
  }

  // TODO: these methods are very similar, can they be merged?

  private def parseProjectBlock(entries: Seq[Entry])(implicit context: BlockContext): ProjectBlock = entries map {
    case Entry.Metadata(metaKey, args) => parseProjectMetadata(metaKey, args)
    case otherEntry                    =>
      // TODO: should this hardcode ${context.tag} as Project? Can we get a link to the relevant part of the docs
      fail(s"Found ${otherEntry.typeName} in ${context.tag} block (${context.block}): `$otherEntry`")
  }

  private def parseTargetBlock(entries: Seq[Entry])(implicit context: BlockContext): TargetBlock = entries map {
    case Entry.Metadata(metaKey, args) => parseTargetMetadata(metaKey, args)
    case otherEntry =>
      fail(s"Found ${otherEntry.typeName} in ${context.tag} block (${context.block}): `$otherEntry`")
  }

  private def parseStructBlock(entries: Seq[Entry])(implicit context: BlockContext): StructBlock = {
    val attributes = mutable.LinkedHashMap[String, Attribute]()
    entries.foreach {
      case Entry.Attribute(key, dataType, annotations) =>
        attributes.safeInsert(key -> parseAttribute(dataType, annotations)(KeyName(key)))
      case otherEntry =>
        // TODO: should this hardcode ${context.tag} as Project? Can we get a link to the relevant part of the docs
        fail(s"Found ${otherEntry.typeName} in ${context.tag} block (${context.block}): `$otherEntry`")
    }
    StructBlock(attributes.toMap)
  }

  /**
    * Turns an AST of a Templefile into a semantic representation
    * @param templefile the AST parsed from the Templefile
    * @return A semantic representation of the project, as well as all the targets and services, defined in the
    *         Templefile
    * @throws SemanticParsingException when there is no project information, as well as when any of the definitions are
    *                                  malformed
    */
  def parseSemantics(templefile: Syntax.Templefile): Templefile = {
    var projectNameBlock: Option[(String, ProjectBlock)] = None

    val targets  = mutable.LinkedHashMap[String, TargetBlock]()
    val services = mutable.LinkedHashMap[String, ServiceBlock]()

    templefile.foreach {
      case DSLRootItem(key, tag, entries) =>
        implicit val blockContext: BlockContext = BlockContext(key, tag)
        tag match {
          // TODO: error message
          case "service" => services.safeInsert(key -> parseServiceBlock(entries))
          case "project" =>
            projectNameBlock.fold { projectNameBlock = Some(key -> parseProjectBlock(entries)) } {
              case (str, _) => fail(s"Multiple projects found: $str and $key")
            }
          // TODO: error message
          case "target" => targets.safeInsert(key -> parseTargetBlock(entries))

          case tag => fail(s"Unknown block type $tag for $key")
        }
    }

    val (projectName, projectBlock) = projectNameBlock.getOrElse { fail("Temple file has no project block") }

    Templefile(projectName, projectBlock, targets.to(ListMap), services.to(ListMap))
  }

}
