import scala.io.Source

object TestUtils {
  // TODO: move to file utils from PR #6
  def readFile(filename: String): String = {
    val file = Source.fromFile(name = filename)
    try file.mkString
    finally file.close
  }

  implicit class FromEither[A, B](either: Either[A, B]) {
    def fromEither(f: A => B): B = either.fold(f, identity)
  }
}
