package temple.DSL.semantics

import temple.DSL.syntax.Args
import temple.utils.MapUtils.FailThrower

object ErrorHandling {

  /**
    * Throws an exception about the semantic analysis.
    * @param str A string representation of the error
    * @return never returns
    * @deprecated use Context.error instead
    */
  private[temple] def fail(str: String): Nothing = throw new SemanticParsingException(str)

  implicit private[temple] val failThrower: FailThrower = fail

  private[temple] def assertNoParameters(args: Args)(implicit context: Context): Unit =
    if (!args.isEmpty) fail(s"Arguments supplied to function $context, which should take no parameters")

}
