package utils

object MonadUtils {

  implicit class FromEither[A, B](either: Either[A, B]) {
    def fromEither(f: A => B): B = either.fold(f, identity)
  }
}
