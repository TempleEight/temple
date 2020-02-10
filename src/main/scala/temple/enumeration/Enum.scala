package temple.enumeration

import temple.utils.MapUtils.FailThrower

/**
  * Wrap an Enum with case-insensitive searching by name and by alias. This is applied to the companion object.
  *
  * @tparam T the class to wrap
  */
trait Enum[T <: EnumEntry] extends enumeratum.Enum[T] {

  /** Lookup an entry in the enum by name, returning None if not found */
  def parseOption(name: String): Option[T] = values.find(_.allNames.contains(name))

  /** Lookup an entry in the enum by name, using a [[FailThrower]] if not found. */
  def parse(name: String)(implicit failThrower: FailThrower): T =
    parseOption(name).getOrElse(failThrower(s"Entry not found: $name. Valid entries are ${values.map(_.name)}"))
}
