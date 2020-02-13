package temple.generate.utils

import temple.utils.StringUtils.indent

import scala.Option.when

/** Any nested sequence of strings that can be flattened and iterated */
trait CodeFormatter {

  /**
    * Iterate through any ordered structure of strings
    * @return An iterator of strings
    */
  def flatIterator: Iterator[String]
}

object CodeFormatter {

  /** Turns a [[String]] into a [[CodeFormatter]], with a trivial iterator of just the single string
    */
  implicit class CodeFormatterString(string: String) extends CodeFormatter {
    override def flatIterator: Iterator[String] = Iterable.single(string).iterator
  }

  /**
    * Turns a list of items into a string with separators, constructed with
    * [[utils.CodeFormatter.mkSQL#list(temple.generate.database.CodeFormatter.CodeFormatterIterableString)]]
    *
    * @param strings a list of strings to be separated
    * @param separator the separator between the items
    */
  class CodeFormatterList(strings: IterableOnce[String], separator: String = ",") extends CodeFormatter {

    override def flatIterator: Iterator[String] = strings.iterator.zipWithIndex.map {
      case (term, i) => mkSQL(when(i != 0)(separator), term)
    }

    /** Add newlines after each separator */
    def spaced: CodeFormatterList = new CodeFormatterList(strings, ",\n")
  }

  /** Turns a [[IterableOnce]] of [[CodeFormatter]]s into a [[CodeFormatter]], with an iterator that visits
    * every child */
  implicit class CodeFormatterIterable(strings: IterableOnce[CodeFormatter]) extends CodeFormatter {
    override def flatIterator: Iterator[String] = strings.iterator.flatMap(_.flatIterator)
  }

  /** An iterable of strings can be treated as an iterable of SQLTerms, but can also be turned into a list. */
  implicit class CodeFormatterIterableString(strings: IterableOnce[String])
      extends CodeFormatterIterable(strings.iterator.map(SQLTermString)) {

    /** Turn a list of SQL terms into a comma-separated list */
    def mkSQLList: CodeFormatterList = new CodeFormatterList(strings)
  }

  object mkSQL {

    /** Turn a list of terms into a comma-separated string */
    def list(terms: CodeFormatterIterableString): String = mkSQL(terms.mkSQLList)

    /** Turn a list of terms into a comma- and newline-separated string */
    def spacedList(terms: CodeFormatterIterableString): String = mkSQL(terms.mkSQLList.spaced)

    /** Combine a sequence of SQL terms into a single string, omitting spaces as necessary */
    def apply(strings: CodeFormatter*): String = {
      val iterator: Iterator[String] = strings.iterator.flatMap(_.flatIterator)

      // Iterate through the strings with a string builder, keeping track of whether the previous segment ends in a
      // space/open-bracket, and so does not need a space inserted.
      val (stringBuilder, _) = iterator.foldLeft((new StringBuilder, true)) {
        case ((acc, noSpace), term) =>
          // Only if the previous segment does not end in a space/open bracket, and the new segment does not start with
          // a space/closing punctuation, do we insert a space
          if (!noSpace && """^[^;,\s)]""".r.unanchored.matches(term)) acc += ' '

          // Append the new string
          acc ++= term

          // Pass forward the string builder and whether we don't need a space next time
          (acc, """[\s(]$""".r.unanchored.matches(term))
      }
      stringBuilder.toString
    }

    /** Construct a SQL statement, like in
      * [[utils.SQLTerm.mkSQL#apply(scala.collection.immutable.Seq)]] but ending in a semicolon */
    def stmt(string: CodeFormatter*): String = mkSQL(string, ";")
  }

  object sqlParens {

    /** Wrap a SQL snippet in parentheses */
    def apply(string: CodeFormatter*): String = mkSQL("(", string, ")")

    /** Wrap a SQL snippet in parentheses, with newlines inside them */
    def spaced(string: CodeFormatter*): String = mkSQL("(", "\n", indent(mkSQL(string)), "\n", ")")
  }
}
