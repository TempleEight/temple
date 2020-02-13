package temple.utils

/** Utility functions useful for performing operations on strings */
object StringUtils {

  /** Given a string, indent it by a given number of spaces */
  def indent(str: String, length: Int = 2): String = str.replaceAll("^|(?<=\n)", " " * length)

  /** Given a string and a wrapping char, append the char at each end */
  def wrap(str: String, char: Char = '"'): String = s"$char$str$char"

  /** Given a string that may contain quotes, automatically escape them */
  def escapeQuotes(str: String): String = str.split('"').mkString("\\\"")
}
