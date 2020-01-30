package temple.DSL

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.parsing.combinator.JavaTokenParsers
import java.io.InputStreamReader

/** A library of parser generators for the Templefile DSL */
protected class DSLParser extends JavaTokenParsers {

  /**
    * A parser generator for repetitions, terminated by a given parser `p0`
    *
    * This is different from `rep` because, unless it finds the termination `q0`, the error message is related to the
    * parsing of `p0` not `q0`
    * @param p0 The parser that is repeated
    * @param q0 The parser marking the end of the sequence. Use `guard` to avoid consuming this input
    * @return A parser that returns a list of the successful parses of `p0`
    */
  def repUntil[T](p0: => Parser[T], q0: => Parser[Any] = ""): Parser[List[T]] = Parser { in =>
    lazy val p = p0
    lazy val q = q0
    val elems  = new ListBuffer[T]

    // run `p0` repeatedly
    @tailrec def applyP(in0: Input): ParseResult[List[T]] = p(in0) match {
      case Success(x, rest) => elems += x; applyP(rest)
      case e: Error         => e // still have to propagate `p`’s parser error
      case Failure(str, input) =>
        q(in0) match {
          case Success(_, next) => Success(elems.toList, next) // allow `q` to consume (use guard to avoid)
          case _: Failure       => Failure(str, input)         // If both fail, propagate `p0`’s failure.
          case e: Error         => e                           // still have to propagate `q0`’s parser error
        }
      case _ => Success(elems.toList, in0)
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
  def repAll[T](p0: => Parser[T]): Parser[List[T]] = repUntil(p0, "$".r)

  /** A parser generator for an entire Templefile */
  def templeFile: Parser[List[DSLRootItem]] = repAll(rootItem)

  /** A parser generator for each item at the root level, i.e. a name, tag and block */
  def rootItem: Parser[DSLRootItem] = (ident <~ ":") ~ (ident <~ "{") ~ repUntil(entry, "}") ^^ {
    case key ~ tag ~ entries => DSLRootItem(key, tag, entries)
  }

  /** A parser generator for an entry within a block. */
  def entry: Parser[Entry] = (attribute | metadata) <~ ";" | rootItem <~ ";".?

  /** A parser generator for a list of arguments in square brackets */
  def listArg: Parser[Arg] = "[" ~> repUntil(arg <~ argsListSeparator, "]") ^^ (elems => Arg.ListArg(elems))

  /** A parser generator for a list of arguments in square brackets, when used in shorthand to replace the brackets */
  def shorthandListArg[T]: Parser[List[Arg] ~ List[T]] = (listArg ^^ (list => List(list))) ~ success(List())

  /** A parser generator for a line of metadata */
  def metadata: Parser[Entry.Metadata] = "#" ~> ident ~ (allArgs | shorthandListArg) ^^ {
    case function ~ (args ~ kwargs) => Entry.Metadata(function, args, kwargs)
  }

  /** A parser generator for a struct’s attribute */
  def attribute: Parser[Entry.Attribute] =
    (ident <~ ":") ~ attributeType ~ repUntil(annotation, guard("[;}]".r)) ^^ {
      case key ~ fieldType ~ annotations => Entry.Attribute(key, fieldType, annotations)
    }

  /** A parser generator for a comma or the end of the argument list */
  def argsListSeparator: Parser[Unit] = ("," | guard("]" | ")" | "}")) ^^^ ()

  /**
    * A parser generator for any argument passed to a type or metadata
    */
  def arg: Parser[Arg] =
    ident ^^ Arg.TokenArg |
    wholeNumber ^^ (str => Arg.IntArg(str.toInt)) |
    floatingPointNumber ^^ (str => Arg.FloatingArg(str.toDouble)) |
    listArg

  /** A parser generator for an argument keyed by a keyword */
  def kwarg: Parser[(String, Arg)] = ((ident <~ ":") ~ arg) ^^ { case ident ~ arg => (ident, arg) }

  /** A parser generator for a sequence of arguments, starting positionally and subsequently keyed */
  def allArgs: Parser[List[Arg] ~ List[(String, Arg)]] =
    "(" ~> (rep(arg <~ argsListSeparator) ~ repUntil(kwarg <~ argsListSeparator, ")"))

  /** A parser generator for the type of an attribute */
  def attributeType: Parser[AttributeType] = ident ~ allArgs.? ^^ {
    case name ~ Some(args ~ kwargs) => AttributeType(name, args, kwargs)
    case name ~ None                => AttributeType(name)
  }

  /** A parser generator for an annotation on an attribute */
  def annotation: Parser[Annotation] = "@" ~> ident ^^ Annotation
}

object DSLParser extends DSLParser {

  /** Show an error message for a failed parse, with the line and number printed */
  protected def printError(input: Input): String = {
    val lineNo = input.pos.line.toString
    lineNo + " | " + input.source.toString.split("\n")(input.pos.line - 1) + "\n" +
    (" " * (input.pos.column + 2 + lineNo.length)) + "^"
  }

  /**
    * Parse the contents of a Temple file
    * @param contents the contents of a Templefile as a string
    * @return Right of the the parsed list of root elements, or Left of a string representing the error
    */
  def parse(contents: String): Either[String, List[DSLRootItem]] =
    parseAll(templeFile, contents) match {
      case Success(result, _)    => Right(result)
      case NoSuccess(str, input) => Left(str + '\n' + printError(input))
    }
}
