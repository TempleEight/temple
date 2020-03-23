package temple.collection.enumeration

import temple.errors.ErrorHandlingContext

trait EnumParser[+T, S, C <: ErrorHandlingContext[Exception]] {

  /** Lookup an entry in the enum by name, throwing a contextual error if not. */
  def parse(name: S)(implicit context: C): T
}
