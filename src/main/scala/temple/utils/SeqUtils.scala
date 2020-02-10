package temple.utils

object SeqUtils {

  implicit class SeqOptionExtras[A](seq: Seq[Option[A]]) {
    def sequence: Option[Seq[A]] = if (seq.contains(None)) None else Some(seq.flatten)
  }
}
