package temple.utils

object ListUtils {

  implicit class ListOptionExtras[A](list: List[Option[A]]) {
    def sequence: Option[List[A]] = if (list.contains(None)) None else Some(list.flatten)
  }
}
