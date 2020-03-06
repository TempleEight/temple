package temple.utils

object MonadUtils {

  /** Adds the method `fromEither` to an Either, à la Haskell, to reduce to an item of the right type */
  implicit class FromEither[A, B](either: Either[A, B]) {

    /** Reduces the Either to an item of the right type, by transforming a Left item if present */
    def fromEither(f: A => B): B = either.fold(f, identity)
  }

  /** Adds the method `matchPartial` to any object, to call a partial function directly on it */
  implicit class MatchPartial[A](x: A) {

    /** Pattern-match on an object with a partial function, returning an option of a result */
    def matchPartial[B](f: PartialFunction[A, B]): Option[B] = f.lift(x)
  }
}
