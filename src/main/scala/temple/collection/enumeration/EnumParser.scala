package temple.collection.enumeration

import temple.errors.ErrorHandlingContext

trait EnumParser[+T, S] {

  /** Lookup an entry in the enum by name, throwing a contextual error if not. */
  def parse(name: S)(implicit context: ErrorHandlingContext): T
}
