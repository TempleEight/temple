package temple.DSL.parser

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.parsing.combinator.RegexParsers

trait UtilParsers extends RegexParsers {

  /**
    * A parser generator for repetitions, terminated by a given parser `p0`
    *
    * This is different from `rep` because, unless it finds the termination `q0`, the error message is related to the
    * parsing of `p0` not `q0`
    * @param p0 The parser that is repeated
    * @param q0 The parser marking the end of the sequence. Use `guard` to avoid consuming this input
    * @return A parser that returns a list of the successful parses of `p0`
    */
  protected def repUntil[T](p0: => Parser[T], q0: => Parser[Any] = ""): Parser[Seq[T]] = Parser { in =>
    lazy val p = p0
    lazy val q = q0
    val elems  = new ListBuffer[T]

    // run `p0` repeatedly
    @tailrec def applyP(in0: Input): ParseResult[Seq[T]] = p(in0) match {
      case Success(x, rest) => elems += x; applyP(rest)
      case e: Error         => e // still have to propagate `p`’s parser error
      case Failure(str, input) =>
        q(in0) match {
          case Success(_, next) => Success(elems.toSeq, next) // allow `q` to consume (use guard to avoid)
          case _: Failure       => Failure(str, input)        // If both fail, propagate `p0`’s failure.
          case e: Error         => e                          // still have to propagate `q0`’s parser error
        }
    }

    applyP(in)
  }

  /**
    * A parser generator for repetitions until the end of the input
    *
    * This is different from `rep` because the error message is related to the parsing of `p0`, rather than stating that
    * the end of the string is expected
    *
    * @param p0 The parser that is repeated
    * @return A parser that returns a list of the successful parses of `p0`
    */
  protected def repAll[T](p0: => Parser[T]): Parser[Seq[T]] = repUntil(p0, "$".r)
}
