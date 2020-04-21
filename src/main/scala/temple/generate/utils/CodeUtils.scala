package temple.generate.utils

object CodeUtils {

  /** Takes an iterable collection of string tuples and pads the first element of each tuple to the length of the
    * longest first element in the collection, returning each */
  def pad(tuples: Iterable[(String, String)], separator: String = " "): Iterable[String] = {
    val padLength = tuples.map(_._1.length).max
    tuples.map { case (first, second) => first.padTo(padLength, ' ') + separator + second }
  }

  def padThree(tuples: Iterable[(String, String, String)]): Iterable[String] = {
    val firstCol       = tuples.map { case (first, _, _) => first }
    val secondThirdCol = pad(tuples.map { case (_, second, third) => (second, third) })
    pad(firstCol.zip(secondThirdCol))
  }
}
