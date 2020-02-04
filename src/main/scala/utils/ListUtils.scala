package utils

import scala.collection.IterableOnce

object ListUtils {

  implicit class IterableOnceExtras[A](iterable: IterableOnce[A]) {

    /** A variant of [[IterableOnce]]'s `mkString` that returns `empty` when the iterable is empty */
    def mkString(start: String, sep: String, end: String, empty: String): String = {
      val it = iterable.iterator
      if (!it.hasNext) empty
      else it.mkString(start, sep, end)
    }

  }

  implicit class ListOptionExtras[A](list: List[Option[A]]) {
    def sequence: Option[List[A]] = if (list.contains(None)) None else Some(list.flatten)
  }
}
