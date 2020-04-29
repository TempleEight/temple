package temple.DSL.semantics

import temple.DSL.semantics.Analyzer.parseParameters
import temple.DSL.syntax.Args
import temple.ast.{ArgType, Metadata}
import temple.collection.enumeration.EnumParser
import temple.errors.ErrorHandlingContext

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

  /** A matcher takes a list of arguments and a semantic context for reporting errors, and returns some metadata */
  private type Matcher = (Args, SemanticContext) => T
  private var matchers = mutable.LinkedHashMap[String, Matcher]()

  /** Take a constructor for metadata, and raise it to an EnumParser (something with a parse method taking a context),
    * by simply ignoring this context */
  def enumParser[A](constructor: A => T): EnumParser[T, A] = new EnumParser[T, A] {
    override def parse(name: A)(implicit context: ErrorHandlingContext): T = constructor(name)
  }

  /**
    * Add a handler for a new type of metadata
    *
    * @param metaKey   The name of the metadata item to add
    * @param argKey The name of the single argument to the metadata. Note that this function is overloaded to with a
    *               single string argument for both `metaKey` and `argKey`
    * @param argType The type of the field to expect
    * @param constructor An EnumParser, i.e. something with a function `parse` that can take a value of the `argType`,
    *                    and return a piece of metadata
    * @tparam A The underlying type of the field, inferred from `argType`
    */
  // TODO: do we need to add support for multiple arguments in future?
  final protected def registerKeywordWithContext[A](
    metaKey: String,
    argKey: String,
    argType: ArgType[A],
  )(constructor: EnumParser[T, A]): Unit =
    matchers += (metaKey -> { (args, context) =>
        implicit val innerContext: SemanticContext = context :+ metaKey
        val argMap                                 = parseParameters(argKey -> None)(args)
        constructor.parse(argMap.getArg(argKey, argType))
      })

  final protected def registerKeyword[A](
    metaKey: String,
    argKey: String,
    argType: ArgType[A],
  )(constructor: A => T): Unit =
    registerKeywordWithContext(metaKey, argKey, argType)(enumParser(constructor))

  final protected def registerKeywordWithContext[A](
    key: String,
    argType: ArgType[A],
  )(constructor: EnumParser[T, A]): Unit =
    registerKeywordWithContext(key, key, argType)(constructor)

  /**
    * Add a handler for a new type of metadata, without an argument.
    *
    * @param metaKey     The name of the metadata item to add
    * @param constructor The item to return
    */
  final protected def registerEmptyKeyword(metaKey: String)(constructor: T): Unit =
    matchers += (metaKey -> { (args, context) =>
        implicit val innerContext: SemanticContext = context :+ metaKey
        MetadataParser.assertNoParameters(args)
        constructor
      })

  /** Perform parsing by looking up the relevant function and passing it the argument list */
  final def apply(metaKey: String, args: Args)(implicit context: SemanticContext): T =
    matchers.get(metaKey).map(matcher => matcher(args, context)) getOrElse {
      context.fail(s"No valid metadata $metaKey")
    }
}

object MetadataParser {

  private[semantics] def assertNoParameters(args: Args)(implicit context: SemanticContext): Unit =
    if (!args.isEmpty) throw context.fail(s"Arguments supplied to function that should take no parameters")
}
