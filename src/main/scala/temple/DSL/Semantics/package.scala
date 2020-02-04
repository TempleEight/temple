package temple.DSL

import temple.DSL.Semantics.ArgType._
import temple.DSL.Semantics.Templefile._
import temple.DSL.Syntax.{Arg, Args, DSLRootItem, Entry}
import utils.ListUtils._

import scala.collection.immutable.ListMap
import scala.collection.{immutable, mutable}

package object Semantics {

  sealed trait Metadata
  sealed trait TargetMetadata            extends Metadata
  sealed trait ServiceMetadata           extends Metadata
  sealed trait ProjectMetadata           extends Metadata
  sealed trait ProjectAndServiceMetadata extends ServiceMetadata with ProjectMetadata
  sealed trait GlobalMetadata            extends TargetMetadata with ProjectAndServiceMetadata

  object Metadata {
    // TODO: is string right here?
    case class Language(name: String)             extends GlobalMetadata
    case class Provider(name: String)             extends ProjectMetadata
    case class Database(name: String)             extends ProjectAndServiceMetadata
    case class ServiceAuth(services: String)      extends ServiceMetadata
    case class TargetAuth(services: List[String]) extends TargetMetadata
    case class Uses(services: List[String])       extends ServiceMetadata
  }

  // TODO: better error handling
  def fail(str: String): Nothing = throw new Error(str)

  def safeInsert[K, V](map: mutable.Map[K, V], kv: (K, V), f: => Nothing): Unit =
    if (map.contains(kv._1)) f else map += kv

  def safeInsert[K, V](map: mutable.Map[K, V], kv: (K, V)): Unit =
    safeInsert(map, kv, fail(s"Key ${kv._1} already exists in $map."))

  sealed abstract class ArgType[T](val stringRep: String) {
    def extractArg(arg: Syntax.Arg): Option[T]
  }

  object ArgType {

    case object IntArgType extends ArgType[Long]("integer") {

      override def extractArg(arg: Arg): Option[Long] =
        arg match { case Arg.IntArg(value) => Some(value); case _ => None }
    }

    case object FloatingArgType extends ArgType[Double]("floating point") {

      override def extractArg(arg: Arg): Option[Double] =
        arg match { case Arg.IntArg(value) => Some(value); case Arg.FloatingArg(value) => Some(value); case _ => None }
    }

    case object TokenArgType extends ArgType[String]("token") {

      override def extractArg(arg: Arg): Option[String] =
        arg match { case Arg.TokenArg(value) => Some(value); case _ => None }
    }

    case object StringArgType extends ArgType[String]("string") {

      override def extractArg(arg: Arg): Option[String] =
        arg match { case Arg.TokenArg(value) => Some(value); case _ => None }
    }

    case class ListArgType[T](elemType: ArgType[T]) extends ArgType[List[T]](s"${elemType.stringRep} list") {

      override def extractArg(arg: Arg): Option[List[T]] =
        arg match { case Arg.ListArg(elems) => elems.map(elemType.extractArg).sequence; case _ => None }
    }
  }

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

  // TODO: refactor for tidiness
  private def parseParameters(specs: (String, Option[Syntax.Arg])*)(args: Args)(implicit context: Context): ArgMap = {
    val specsMap = specs.to(ListMap)
    val argc     = args.posargs.length

    if (specs.sizeIs < argc)
      fail(s"Too many arguments supplied to function $context (found $argc, expected at most ${specs.length})")

    val map = new mutable.HashMap[String, Syntax.Arg]()
    (specsMap, args.posargs).zipped foreach {
      case ((name, _), arg) =>
        safeInsert(map, name -> arg, fail(s"Programmer error: duplicate argument name $name in spec for $context?"))
    }
    args.kwargs foreach {
      case (name, arg) =>
        if (!specsMap.contains(name)) fail(s"Unknown keyword argument $name with value $arg for $context")
        safeInsert(map, name -> arg, fail(s"Duplicate argument provided for $name for $context"))
    }
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

  implicit private class ArgMap(argMap: Map[String, Arg]) {

    def getArg[T](key: String, argType: ArgType[T])(implicit context: Context): T =
      argType.extractArg(argMap(key)).getOrElse {
        fail(s"${argType.stringRep.capitalize} expected at $key for $context, found ${argMap(key)}")
      }

    def getOptionArg[T](key: String, argType: ArgType[T])(implicit context: Context): Option[T] =
      argMap(key) match { case Arg.NoArg => None; case _ => Some(getArg(key, argType)) }
  }

  // TODO: this is too messy
  def parseAttributeType(
    dataType: Syntax.AttributeType,
    annotations: List[Syntax.Annotation]
  )(implicit keyNameContext: KeyName): AttributeType = dataType match {
    case Syntax.AttributeType(typeName, args) =>
      implicit val context: Context = Context(s"$typeName@$keyNameContext")

      typeName match {
        case "int" =>
          val argMap = parseParameters(
            "max"       -> Some(Arg.NoArg),
            "min"       -> Some(Arg.NoArg),
            "precision" -> Some(Arg.IntArg(4))
          )(args)
          AttributeType.IntType(
            argMap.getOptionArg("min", ArgType.IntArgType),
            argMap.getOptionArg("max", ArgType.IntArgType),
            argMap.getArg("precision", ArgType.IntArgType).toShort
          )
        case "string" =>
          val argMap = parseParameters(
            "maxLength" -> Some(Arg.NoArg),
            "minLength" -> Some(Arg.IntArg(0))
          )(args)
          AttributeType.StringType(
            argMap.getOptionArg("maxLength", ArgType.IntArgType),
            argMap.getArg("minLength", ArgType.IntArgType).toInt
          )
        case "float" =>
          val argMap = parseParameters(
            "max"       -> Some(Arg.FloatingArg(Double.MaxValue)),
            "min"       -> Some(Arg.FloatingArg(Double.MaxValue)),
            "precision" -> Some(Arg.IntArg(8))
          )(args)
          AttributeType.FloatType(
            argMap.getArg("max", ArgType.FloatingArgType),
            argMap.getArg("min", ArgType.FloatingArgType),
            argMap.getArg("precision", ArgType.FloatingArgType).toShort
          )
        case "date"     => AttributeType.DateType
        case "datetime" => AttributeType.DateTimeType
        case "time"     => AttributeType.TimeType
        case "data" =>
          val argMap = parseParameters(
            "maxSize" -> Some(Arg.NoArg)
          )(args)
          AttributeType.BlobType(
            argMap.getOptionArg("maxSize", IntArgType)
          )
        case "bool"   => AttributeType.BoolType
        case typeName => fail(s"Unknown type $typeName")
        //TODO: handle annotations
      }
  }

  class MetadataParser[T <: Metadata]() {
    private var matcher: (String, Args) => Option[T] = (_, _) => None

    final protected object inherit {

      def from[A <: T](parser: MetadataParser[A]): Unit = {
        val prevMatcher = matcher
        matcher = { (metaKey, args) =>
          prevMatcher(metaKey, args) orElse parser.matcher(metaKey, args)
        }
      }
    }

    final protected def registerKeyword[A](
      metaKey: String,
      argKey: String,
      argType: ArgType[A],
      constructor: A => T
    ): Unit = {
      val prevMatcher = matcher
      matcher = { (inputMetaKey, args) =>
        implicit val context: Context = Context(metaKey)
        if (inputMetaKey == metaKey) {
          val argMap = parseParameters(argKey -> None)(args)
          Some(constructor(argMap.getArg(argKey, argType)))
        } else prevMatcher(inputMetaKey, args)
      }
    }

    final protected def registerKeyword[A](key: String, argType: ArgType[A], constructor: A => T): Unit =
      registerKeyword(key, key, argType, constructor)

    final def apply(metaKey: String, args: Args): T = matcher(metaKey, args).getOrElse {
      fail("TODO: error message")
    }
  }

  private val parseGlobalMetadata = new MetadataParser[GlobalMetadata] {
    registerKeyword("language", TokenArgType, Metadata.Language)
  }

  private val parseProjectAndServiceMetadata = new MetadataParser[ProjectAndServiceMetadata] {
    inherit from parseGlobalMetadata
    registerKeyword("database", TokenArgType, Metadata.Database)
  }

  private val parseServiceMetadata = new MetadataParser[ServiceMetadata] {
    inherit from parseProjectAndServiceMetadata
    registerKeyword("auth", "login", TokenArgType, Metadata.ServiceAuth)
    registerKeyword("uses", "services", ListArgType(TokenArgType), Metadata.Uses)
  }

  private val parseProjectMetadata = new MetadataParser[ProjectMetadata] {
    inherit from parseProjectAndServiceMetadata
    registerKeyword("provider", TokenArgType, Metadata.Provider)
  }

  private val parseTargetMetadata = new MetadataParser[TargetMetadata] {
    inherit from parseGlobalMetadata
    registerKeyword("auth", "services", ListArgType(TokenArgType), Metadata.TargetAuth)
  }

  private def parseStructBlock(entries: List[Entry])(implicit context: BlockContext): StructBlock = {
    val attributes = mutable.LinkedHashMap[String, AttributeType]()
    entries.foreach {
      case Entry.Attribute(key, dataType, annotations) =>
        safeInsert(attributes, key -> parseAttributeType(dataType, annotations)(KeyName(key)))
      case otherEntry =>
        // TODO: should this hardcode ${context.tag} as Project? Can we get a link to the relevant part of the docs
        fail(s"Found ${otherEntry.typeName} in ${context.tag} block (${context.block}): `$otherEntry`")
    }
    StructBlock(attributes.toMap)
  }

  private def parseServiceBlock(entries: List[Entry])(implicit context: BlockContext): ServiceBlock = {
    val attributes = mutable.LinkedHashMap[String, AttributeType]()
    val metadatas  = mutable.ListBuffer[ServiceMetadata]()
    val structs    = mutable.LinkedHashMap[String, StructBlock]()
    entries.foreach {
      case Entry.Attribute(key, dataType, annotations) =>
        safeInsert(attributes, key -> parseAttributeType(dataType, annotations)(KeyName(key)))
      case Entry.Metadata(metaKey, args) => metadatas += parseServiceMetadata(metaKey, args)
      case DSLRootItem(key, tag, entries) =>
        safeInsert(structs, key -> parseStructBlock(entries)(BlockContext(key, tag, context))) // TODO: support structs
    }
    ServiceBlock(attributes.to(ListMap), metadatas.toList, structs.to(ListMap))
  }

  private def parseProjectBlock(entries: List[Entry])(implicit context: BlockContext): ProjectBlock = entries map {
    case Entry.Metadata(metaKey, args) => parseProjectMetadata(metaKey, args)
    case otherEntry                    =>
      // TODO: should this hardcode ${context.tag} as Project? Can we get a link to the relevant part of the docs
      fail(s"Found ${otherEntry.typeName} in ${context.tag} block (${context.block}): `$otherEntry`")
  }

  private def parseTargetBlock(entries: List[Entry])(implicit context: BlockContext): TargetBlock = entries map {
    case Entry.Metadata(metaKey, args) => parseTargetMetadata(metaKey, args)
    case otherEntry =>
      fail(s"Found ${otherEntry.typeName} in ${context.tag} block (${context.block}): `$otherEntry`")
  }

  def parseSemantics(templefile: Syntax.Templefile): Semantics.Templefile = {
    var projectNameBlock: Option[(String, ProjectBlock)] = None

    val targets  = mutable.LinkedHashMap[String, TargetBlock]()
    val services = mutable.LinkedHashMap[String, ServiceBlock]()

    templefile.foreach {
      case DSLRootItem(key, tag, entries) =>
        implicit val blockContext: BlockContext = BlockContext(key, tag)
        tag match {
          // TODO: error message
          case "service" => safeInsert(services, key -> parseServiceBlock(entries))
          case "project" =>
            projectNameBlock.fold { projectNameBlock = Some(key -> parseProjectBlock(entries)) } {
              case (str, _) => fail(s"Multiple projects found: $str and $key")
            }
          // TODO: error message
          case "target" => safeInsert(targets, key -> parseTargetBlock(entries))

          case tag => fail(s"Unknown block type $tag for $key")
        }
    }

    // TODO: better errors
    val (projectName, projectBlock) = projectNameBlock.getOrElse { fail("no project block") }

    Templefile(projectName, projectBlock, targets.to(ListMap), services.to(ListMap))
  }
}
