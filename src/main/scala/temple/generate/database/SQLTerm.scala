package temple.generate.database

import temple.utils.StringUtils.indent

import scala.Option.when

/** Any nested sequence of strings that can be flattened and iterated */
trait SQLTerm {

  /**
    * Iterate through any ordered structure of strings
    * @return An iterator of strings
    */
  def flatIterator: Iterator[String]
}

object SQLTerm {

  /** Turns a [[String]] into a [[temple.generate.database.SQLTerm]], with a trivial iterator of just the single string
    */
  implicit class SQLTermString(string: String) extends SQLTerm {
    override def flatIterator: Iterator[String] = Iterable.single(string).iterator
  }

  class SQLTermList(strings: IterableOnce[SQLTerm], spacing: String = ",") extends SQLTerm {

    override def flatIterator: Iterator[String] = strings.iterator.zipWithIndex.map {
      case (term, i) => mkSQL(when(i != 0)(spacing), term)
    }

    def spaced: SQLTermList = new SQLTermList(strings, ",\n")
  }

  implicit class SQLTermIterable(strings: IterableOnce[SQLTerm]) extends SQLTerm {
    override def flatIterator: Iterator[String] = strings.iterator.flatMap(_.flatIterator)

    def mkSQLList: SQLTermList = new SQLTermList(strings)
  }

  implicit class SQLTermIterableString(strings: IterableOnce[String])
      extends SQLTermIterable(strings.iterator.map(SQLTermString))

  object mkSQL {
    def list(terms: SQLTermIterable): String       = mkSQL(terms.mkSQLList)
    def spacedList(terms: SQLTermIterable): String = mkSQL(terms.mkSQLList.spaced)

    def apply(strings: SQLTerm*): String = {
      val iterator: Iterator[String] = strings.iterator.flatMap(_.flatIterator)
      val (_, stringBuilder) = iterator
        .foldLeft((true, new StringBuilder)) {
          case ((noSpace, acc), term) =>
            if (!noSpace && """^[^;,\s\)]""".r.unanchored.matches(term)) acc += ' '
            acc ++= term
            ("""[\s\(]$""".r.unanchored.matches(term), acc)
        }
      stringBuilder.toString()
    }

    def stmt(string: SQLTerm*): String = mkSQL(string, ";")
  }

  object sqlParens {
    def apply(string: SQLTerm*): String  = mkSQL("(", string, ")")
    def spaced(string: SQLTerm*): String = mkSQL("(", "\n", indent(mkSQL(string)), "\n", ")")
  }
}
