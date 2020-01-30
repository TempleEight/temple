package utils

/** Utility functions useful for performing operations on strings */
object StringUtils {

  /** Given a string, indent it by a given number of spaces */
  def indent(str: String, length: Int = 2): String = str.replaceAll("^|(?<=\n)", " " * length)

  implicit class IterableOnceImprovements[A](collection: IterableOnce[A]) {

    /** A variant of [[IterableOnce]]'s `mkString` that returns `empty` when the iterable is empty */
    def mkString(start: String, sep: String, end: String, empty: String): String = {
      val it = collection.iterator
      if (!it.hasNext) empty
      else it.mkString(start, sep, end)
    }
  }
}
