package temple.collection.enumeration

import temple.errors.ErrorHandlingContext

/**
  * Wrap an Enum with case-insensitive searching by name and by alias. This is applied to the companion object.
  *
  * @tparam T the class to wrap
  */
trait Enum[T <: EnumEntry] extends enumeratum.Enum[T] with EnumParser[T, String] {

  /** Lookup an entry in the enum by name, returning None if not found */
  def parseOption(name: String): Option[T] = values.find(_.allNames.contains(name.toLowerCase))

  /** Lookup an entry in the enum by name, throwing a contextual error if not. */
  def parse(name: String)(implicit context: ErrorHandlingContext): T =
    parseOption(name).getOrElse { context.fail(s"Entry not found: $name. Valid entries are ${values.map(_.name)}.") }
}
