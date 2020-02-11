package temple.utils

object SeqUtils {

  /**
    * Add a `sequence` method to a sequence of options, to collapse into an option of a list
    * @param seq the sequence of options
    * @tparam A the underlying type of the contents of the sequence
    */
  implicit class SeqOptionExtras[A](seq: Seq[Option[A]]) {

    /** Take a sequence of options and return None if any items are none, otherwise return Some of the items.
      * Analogue to Haskell's `Prelude.sequence`. */
    def sequence: Option[Seq[A]] = if (seq.contains(None)) None else Some(seq.flatten)
  }
}
