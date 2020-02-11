package temple.DSL.semantics

import temple.DSL.semantics.Analyser.parseParameters
import temple.DSL.semantics.ErrorHandling.{BlockContext, Context, fail}
import temple.DSL.syntax.Args
import temple.utils.MapUtils._

import scala.collection.mutable

/**
  * Build a metadata parser by building a list of function calls for each match
  *
  * This is needed because each call to [[temple.DSL.semantics.MetadataParser#registerKeyword]] is effectively
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
    *               [[temple.DSL.semantics.MetadataParser#registerKeyword]]
    * @param argType The type of the field to expect
    * @param constructor The function to turn an input of type [[ArgType]] into a value of type [[T]]
    * @tparam A The underlying type of the field, inferred from `argType`
    */
  // TODO: do we need to add support for multiple arguments in future?
  final protected def registerKeyword[A](metaKey: String, argKey: String, argType: ArgType[A])(
    constructor: A => T,
  ): Unit =
    matchers += (metaKey -> { args =>
        implicit val context: Context = Context(metaKey)
        val argMap                    = parseParameters(argKey -> None)(args)
        constructor(argMap.getArg(argKey, argType))
      })

  /** A shorthand for [[temple.DSL.semantics.MetadataParser#registerKeyword]] with the same `key` used for both the
    * metadata name and its single argument */
  final protected def registerKeyword[A](key: String, argType: ArgType[A])(constructor: A => T): Unit =
    registerKeyword(key, key, argType)(constructor)

  /** Perform parsing by looking up the relevant function and
    * [[scala.collection.IterableOnceOps#foldRight(java.lang.Object, scala.Function2)]] supports */
  final def apply(metaKey: String, args: Args)(implicit context: BlockContext): T =
    matchers.get(metaKey).map(_(args)) getOrElse {
      fail(s"No valid metadata $metaKey in $context")
    }
}
