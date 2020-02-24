package temple.generate.utils

import temple.utils.StringUtils.indent

import scala.Option.when

/** Any nested sequence of strings that can be flattened and iterated */
trait CodeTerm {

  /**
    * Iterate through any ordered structure of strings
    * @return An iterator of strings
    */
  def flatIterator: Iterator[String]
}

object CodeTerm {

  /** Turns a [[String]] into a [[CodeTerm]], with a trivial iterator of just the single string
    */
  implicit class CodeTermString(string: String) extends CodeTerm {
    override def flatIterator: Iterator[String] = Iterable.single(string).iterator
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
      extends CodeTermIterable(strings.iterator.map(CodeTermString)) {

    /** Turn a list of code terms into a comma-separated list */
    def mkCodeList: CodeTermList = new CodeTermList(strings)
  }

  object mkCode {

    /** Turn a list of terms into a comma-separated string */
    def list(terms: CodeTermIterableString): String = mkCode(terms.mkCodeList)

    /** Turn a list of terms into a comma- and newline-separated string */
    def spacedList(terms: CodeTermIterableString): String = mkCode(terms.mkCodeList.spaced)

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

  sealed class codeWrap private (start: String, end: String) {

    /** Wrap a code snippet in parentheses */
    def apply(string: CodeTerm*): String = mkCode(start, string, end)

    /** Wrap a code snippet in parentheses, with newlines inside them */
    def spaced(string: CodeTerm*): String = mkCode(start, "\n", indent(mkCode(string)), "\n", end)
  }

  object codeWrap {

    object parens extends codeWrap("(", ")") {
      object block extends codeWrap("(\n", ")\n")
    }

    object curly extends codeWrap("{", "}") {
      object block extends codeWrap("{\n", "}\n")
    }

    object square extends codeWrap("[", "]") {
      object block extends codeWrap("[\n", "]\n")
    }
  }

  type StringWrap = String => String
  private def stringWrap(start: String, end: String)(string: String): String = start + string + end
  private def stringWrap(start: String): StringWrap                          = stringWrap(start, start)

  /** Wrap a string in double quotes. Note that this does not perform any escaping */
  val doubleQuote: StringWrap = stringWrap("\"")

  /** Wrap a string in single quotes. Note that this does not perform any escaping */
  val singleQuote: StringWrap = stringWrap("\'")
}
