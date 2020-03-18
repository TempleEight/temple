package temple.DSL.semantics

import temple.DSL.semantics.Analyser.parseParameters
import temple.DSL.semantics.ErrorHandling.{BlockContext, Context, fail}
import temple.DSL.syntax.Arg.NoArg
import temple.DSL.syntax.Args
import temple.ast.{ArgType, Metadata}

import scala.collection.mutable

/**
  * Build a metadata parser by building a list of function calls for each match
  *
  * This is needed because each call to [[MetadataParser#registerKeyword]] is effectively
  * dependently typed, so it cannot be encoded into a tuple, but instead must immediately be bundled into a function
  *
  * @tparam T the subtype of metadata that we are specifically parsing.
  */
class MetadataParser[T <: Metadata]() {
  private type Matcher = Args => T
  private var matchers = mutable.LinkedHashMap[String, Matcher]()

  /**
    * Add a handler for a new type of metadata
    *
    * @param metaKey The name of the metadata item to add
    * @param argKey The name of the single argument to the metadata. Note that there is also
    *               [[MetadataParser#registerKeyword(java.lang.String, temple.DSL.semantics.ArgType, scala.Option, scala.Function1)]]
    *               if this is the same as `metaKey`.
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

  /** A shorthand for
    * [[MetadataParser#registerKeyword(java.lang.String, temple.DSL.semantics.ArgType, scala.Option, scala.Function1)]]
    * with the same `key` used for both the metadata name and its single argument */
  final protected def registerKeyword[A](key: String, argType: ArgType[A])(constructor: A => T): Unit =
    registerKeyword(key, key, argType)(constructor)

  /**
    * Add a handler for a new type of metadata, with an optional argument.
    *
    * @param metaKey The name of the metadata item to add
    * @param argKey The name of the single argument to the metadata.
    * @param argType The type of the field to expect
    * @param constructor The function to turn an optional input of type [[ArgType]] into a value of type [[T]]
    * @tparam A The underlying type of the field, inferred from `argType`
    */
  final protected def registerOptionalKeyword[A](metaKey: String, argKey: String, argType: ArgType[A])(
    constructor: Option[A] => T,
  ): Unit =
    matchers += (metaKey -> { args =>
        implicit val context: Context = Context(metaKey)
        val argMap                    = parseParameters(argKey -> Some(NoArg))(args)
        constructor(argMap.getOptionArg(argKey, argType))
      })

  /** Perform parsing by looking up the relevant function and passing it the argument list */
  final def apply(metaKey: String, args: Args)(implicit context: BlockContext): T =
    matchers.get(metaKey).map(_(args)) getOrElse {
      fail(s"No valid metadata $metaKey in $context")
    }
}
