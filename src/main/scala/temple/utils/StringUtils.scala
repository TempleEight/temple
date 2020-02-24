package temple.utils

/** Utility functions useful for performing operations on strings */
object StringUtils {

  private def indentLine(line: String, indent: String): String = if (line.isBlank) line else indent + line

  /** Given a string, indent it by a given number of spaces */
  def indent(str: String, length: Int = 2): String =
    str
    // limit is -1 to avoid gobbling trailing newlines
    // https://stackoverflow.com/questions/27689065/how-to-split-string-with-trailing-empty-strings-in-result
      .split("\n", -1)
      .map(indentLine(_, " " * length))
      .mkString("\n")

  /** Given a string, convert it to snake case */
  // https://github.com/lift/framework/blob/f1b450db2dd6a22cf9ffe5576ec34c8e87118319/core/util/src/main/scala/net/liftweb/util/StringHelpers.scala#L91
  def snakeCase(str: String): String =
    str
      .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
      .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
      .toLowerCase
}
