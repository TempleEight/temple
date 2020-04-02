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

  // https://scalac.io/typeclasses-in-scala/
  trait CodeTermClass[-C] {
    def flatIterator(code: C): Iterator[String]
  }

  /** Add the flatIterator method to anything that is a codeterm*/
  implicit class CodeTermOps[C](codeTerm: C)(implicit codeTermClass: CodeTermClass[C]) extends CodeTerm {
    def flatIterator: Iterator[String] = codeTermClass.flatIterator(codeTerm)
  }

  /**
    * Turns a list of items into a string with separators, constructed with
    * [[temple.generate.utils.CodeTerm.mkCode#list(scala.collection.immutable.Seq)]]
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

  /** Turns a [[String]] into a [[CodeTerm]], with a trivial iterator of just the single string */
  implicit val stringCodeTerm: CodeTermClass[String] = Iterable.single(_).iterator

  /** Turns a [[None]] into a [[CodeTerm]], with a trivial iterator of nothing */
  implicit val noneCodeTerm: CodeTermClass[None.type] = _ => Iterator.empty

  /** Turns an [[IterableOnce]] into a [[CodeTerm]], with a trivial iterator of nothing */
  implicit def iterableCodeTerm[C: CodeTermClass]: CodeTermClass[IterableOnce[C]] = _.iterator.flatMap(_.flatIterator)

  /** Turns a [[CodeTerm]] into a [[CodeTerm]], necessary for nested code terms */
  implicit val codeTermCodeTerm: CodeTermClass[CodeTerm] = _.flatIterator

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
