package utils

/** Utility functions useful for performing operations on strings */
object StringUtils {

  /** Given a string, indent it by a given number of spaces */
  def indent(str: String, length: Int = 2): String = str.replaceAll("^|(?<=\n)", " " * length)
}
