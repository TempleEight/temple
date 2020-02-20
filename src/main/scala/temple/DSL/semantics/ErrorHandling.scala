package temple.DSL.semantics

import temple.DSL.syntax.Args
import temple.utils.MapUtils.FailThrower

object ErrorHandling {

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
  private[semantics] def fail(str: String): Nothing = throw new SemanticParsingException(str)

  implicit private[semantics] val failThrower: FailThrower = fail

  // TODO: add more path
  private[semantics] case class KeyName(keyName: String)  { override def toString: String = keyName  }
  private[semantics] case class Context(function: String) { override def toString: String = function }

  private[semantics] case class BlockContext private (block: String, tag: String, context: Option[BlockContext]) {

    override def toString: String = context.fold(s"$tag ($block)") { context =>
      s"$tag ($block, inside $context)"
    }
  }

  private[semantics] object BlockContext {
    def apply(block: String, tag: String, wrapper: BlockContext): BlockContext = BlockContext(block, tag, Some(wrapper))
    def apply(block: String, tag: String): BlockContext                        = BlockContext(block, tag, None)
  }

  private[semantics] def assertNoParameters(args: Args)(implicit context: Context): Unit =
    if (!args.isEmpty) fail(s"Arguments supplied to function $context, which should take no parameters")

}
