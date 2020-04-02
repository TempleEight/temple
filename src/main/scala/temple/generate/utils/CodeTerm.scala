package temple.generate.utils

import temple.generate.utils.CodeTerm.CodeTermList
import temple.utils.StringUtils.{indent, tabIndent}

import scala.Option.when

/** Any nested sequence of strings that can be flattened and iterated */
trait CodeTerm {

  /**
    * Iterate through any ordered structure of strings
    * @return An iterator of strings
    */
  def flatIterator: Iterator[String]

  /** Turn a list of code terms into a comma-separated list */
  def mkCodeList: CodeTermList = new CodeTermList(flatIterator)
}

object CodeTerm {

  /** Turns a [[String]] into a [[CodeTerm]], with a trivial iterator of just the single string */
  implicit class CodeTermString(string: String) extends CodeTerm {
    override def flatIterator: Iterator[String] = Iterable.single(string).iterator
  }

  /** Turns a [[None]] into a [[CodeTerm]], with a trivial iterator of nothing */
  implicit class CodeTermNone(none: None.type) extends CodeTerm {
    override def flatIterator: Iterator[String] = Iterator.empty
  }

  /**
    * Turns a list of items into a string with separators, constructed with
    * [[temple.generate.utils.CodeTerm.mkCode#list(temple.generate.utils.CodeTerm.CodeTermIterableString)]]
    *
    * @param strings a list of strings to be separated
    * @param separator the separator between the items
    */
  class CodeTermList(strings: IterableOnce[String], separator: String = ",") extends CodeTerm {

    override def flatIterator: Iterator[String] = strings.iterator.zipWithIndex.map {
      case (term, i) => mkCode(when(i != 0)(separator), term)
    }

    /** Add newlines after each separator */
    def spaced: CodeTermList = new CodeTermList(strings, ",\n")

  }

  /** Turns a [[IterableOnce]] of [[CodeTerm]]s into a [[CodeTerm]], with an iterator that visits
    * every child */
  implicit class CodeTermIterable(strings: IterableOnce[CodeTerm]) extends CodeTerm {
    override def flatIterator: Iterator[String] = strings.iterator.flatMap(_.flatIterator)
  }

  /** An iterable of strings can be treated as an iterable of CodeTerms, but can also be turned into a list. */
  implicit class CodeTermIterableString(strings: IterableOnce[String])
      extends CodeTermIterable(strings.iterator.map(CodeTermString))

  implicit class CodeTermDoubleIterable(options: IterableOnce[IterableOnce[String]]) extends CodeTerm {
    override def flatIterator: Iterator[String] = options.iterator.flatMap(_.flatIterator)
  }

  object mkCode {

    /** Turn a list of terms into a comma-separated string */
    def list(terms: CodeTerm*): String = mkCode(terms.mkCodeList)

    /** Turn a list of terms into a comma- and newline-separated string */
    def spacedList(terms: CodeTerm*): String = mkCode(terms.mkCodeList.spaced)

    /** Turn a list of terms into an escaped newline-seperated string */
    def shellLines(terms: CodeTerm*): String = mkCode(new CodeTermList(terms.flatIterator, " \\\n  "))

    /** Turn a list of terms into newlines */
    def lines(terms: CodeTerm*): String = mkCode(new CodeTermList(terms.flatIterator, "\n"))

    /** Turn a list of terms into double-newlines */
    def doubleLines(terms: CodeTerm*): String = mkCode(new CodeTermList(terms.flatIterator, "\n\n"))

    /** Combine a sequence of code terms into a single string, omitting spaces as necessary */
    def apply(strings: CodeTerm*): String = {
      val iterator: Iterator[String] = strings.iterator.flatMap(_.flatIterator)

      // Iterate through the strings with a string builder, keeping track of whether the previous segment ends in a
      // space/open-bracket, and so does not need a space inserted.
      val (stringBuilder, _) = iterator.foldLeft((new StringBuilder, true)) {
        case ((acc, noSpace), term) =>
          // Only if the previous segment does not end in a space/open bracket, and the new segment does not start with
          // a space/closing punctuation, do we insert a space
          if (!noSpace && """^[^;,:\]}\s)]""".r.unanchored.matches(term)) acc += ' '

          // Append the new string
          acc ++= term

          // Pass forward the string builder and whether we don't need a space next time
          (acc, """[\s({\[]$""".r.unanchored.matches(term))
      }
      stringBuilder.toString
    }

    /** Construct a code statement, like in
      * [[temple.generate.utils.CodeTerm.mkCode#apply(scala.collection.immutable.Seq)]] but ending in a semicolon */
    def stmt(string: CodeTerm*): String = mkCode(string, ";")
  }

  sealed class CodeWrap private (start: String, end: String) {

    /** Add a string before the opening symbol, without any spacing */
    def prefix(prefix: String): CodeWrap = new CodeWrap(prefix + start, end)

    /** Wrap a (space-separated list of) terms in parentheses */
    def apply(string: CodeTerm*): String = mkCode(start, string, end)

    /** Wrap a (comma-separated list of) terms in parentheses */
    def list(string: CodeTerm*): String = mkCode(start, mkCode.list(string), end)

    /** Wrap a (comma-separated list of) code snippet in parentheses, indenting with spaces */
    def spacedList(string: CodeTerm*): String = mkCode(start, "\n", indent(mkCode.spacedList(string)), "\n", end)

    /** Wrap a (newline-separated list of) code snippet in parentheses, indenting with spaces */
    def spaced(string: CodeTerm*): String = mkCode(start, "\n", indent(mkCode.lines(string)), "\n", end)

    /** Wrap a (comma-separated list of) code snippet in parentheses, indenting with tabs */
    def tabbedList(string: CodeTerm*): String = mkCode(start, "\n", tabIndent(mkCode.spacedList(string)), "\n", end)

    /** Wrap a (newline-separated list of) code snippet in parentheses, indenting with tabs */
    def tabbed(string: CodeTerm*): String = mkCode(start, "\n", tabIndent(mkCode.lines(string)), "\n", end)

    /** Wrap a (comma-separated list of) code snippet in parentheses, with no indent */
    def noIndentList(string: CodeTerm*): String = mkCode(start, "\n", mkCode.spacedList(string), "\n", end)

    /** Wrap a (newline-separated list of) code snippet in parentheses, with no indent */
    def noIndent(string: CodeTerm*): String = mkCode(start, "\n", mkCode.lines(string), "\n", end)

    /** Wrap a (trailing-comma-separated list of) code snippet in parentheses, indenting with spaces */
    def spacedTrailingList(string: CodeTerm*): String =
      mkCode(start, "\n", indent(mkCode.spacedList(string)), ",\n", end)

    /** Wrap a (trailing-comma-separated list of) code snippet in parentheses, indenting with tabs */
    def tabbedTrailingList(string: CodeTerm*): String =
      mkCode(start, "\n", tabIndent(mkCode.spacedList(string)), ",\n", end)
  }

  object CodeWrap {
    val parens = new CodeWrap("(", ")")
    val curly  = new CodeWrap("{", "}")
    val square = new CodeWrap("[", "]")
  }

}
