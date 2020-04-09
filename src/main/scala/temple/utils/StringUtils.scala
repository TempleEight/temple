package temple.utils

import scala.util.Random

/** Utility functions useful for performing operations on strings */
object StringUtils {

  private def indentLine(line: String, indent: String): String = if (line.isBlank) line else indent + line

  /** Given a string, indent it by a given indent string */
  private def indent(str: String, indentStr: String): String =
    str
    // limit is -1 to avoid gobbling trailing newlines
    // https://stackoverflow.com/questions/27689065/how-to-split-string-with-trailing-empty-strings-in-result
      .split("\n", -1)
      .map(indentLine(_, indentStr))
      .mkString("\n")

  /** Given a string, indent it by a given number of spaces */
  def indent(str: String, length: Int = 2): String = indent(str, " " * length)

  /** Given a string, indent it by a given number of tabs */
  def tabIndent(str: String, length: Int = 1): String = indent(str, "\t" * length)

  /** Given a string, convert it to snake case */
  // https://github.com/lift/framework/blob/f1b450db2dd6a22cf9ffe5576ec34c8e87118319/core/util/src/main/scala/net/liftweb/util/StringHelpers.scala#L91
  def snakeCase(str: String, sep: Char = '_'): String =
    str
      .replaceAll("([A-Z]+)([A-Z][a-z])", s"$$1$sep$$2")
      .replaceAll("([a-z\\d])([A-Z])", s"$$1$sep$$2")
      .toLowerCase

  def kebabCase(str: String): String = snakeCase(str, '-')

  /** Generate a random alphanumeric string of given length */
  def randomString(length: Int): String =
    new Random().alphanumeric.take(length).mkString

  /** Convert the first character/acronym of a string to lowercase */
  def decapitalize(string: String): String =
    // The first capital of the string, optionally followed by any subsequent capitals except for those starting a word
    """^[A-Z]([A-Z]+(?=[^a-z]))?""".r.replaceAllIn(string, leadingCapitals => leadingCapitals.group(0).toLowerCase)

  type StringWrap = String => String

  private def stringWrap(start: String, end: String)(string: String): String = start + string + end

  private def stringWrap(start: String): StringWrap = stringWrap(start, start)

  /** Wrap a string in double quotes, note that this does not perform any escaping */
  val doubleQuote: StringWrap = stringWrap("\"")

  /** Wrap a string in single quotes, note that this does not perform any escaping */
  val singleQuote: StringWrap = stringWrap("\'")

  /** Wrap a string in backticks, note that this does not perform any escaping */
  val backTick: StringWrap = stringWrap("`")

  /** Wrap a string in Spanish question marks, tenga en cuenta que esto no realiza ningún escape */
  val españolQue: StringWrap = stringWrap("¿", "?")

}
