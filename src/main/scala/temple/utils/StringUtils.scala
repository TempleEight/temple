package temple.utils

/** Utility functions useful for performing operations on strings */
object StringUtils {

  /** Given a string, indent it by a given number of spaces */
  def indent(str: String, length: Int = 2): String = str.replaceAll("^|(?<=\n)", " " * length)

  /** Given a string, convert it to snake case */
  // https://github.com/lift/framework/blob/f1b450db2dd6a22cf9ffe5576ec34c8e87118319/core/util/src/main/scala/net/liftweb/util/StringHelpers.scala#L91
  def snakeCase(str: String): String =
    str
      .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
      .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
      .toLowerCase
}
