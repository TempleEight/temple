package temple.generate.utils

object CodeUtils {

  /** Takes an iterable collection of string tuples and pads the first element of each tuple to the length of the
    * longest first element in the collection */
  def pad(tuples: Iterable[(String, String)]): Iterable[(String, String)] = {
    val padLength = tuples.map(_._1.length).max + 1
    tuples.map { case (first, second) => (first.padTo(padLength, ' '), second) }
  }
}
