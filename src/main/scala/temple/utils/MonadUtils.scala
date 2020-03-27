package temple.utils

object MonadUtils {

  /** Adds the method `fromEither` to an Either, à la Haskell, to reduce to an item of the right type */
  implicit class FromEither[A, B](either: Either[A, B]) {

    /** Reduces the Either to an item of the right type, by transforming a Left item if present */
    def fromEither[B1 >: B](f: A => B1): B1 = either.fold(f, identity)
  }
}
