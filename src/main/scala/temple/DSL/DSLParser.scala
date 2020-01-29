package temple.DSL

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.parsing.combinator.JavaTokenParsers
import java.io.InputStreamReader

protected class DSLParser extends JavaTokenParsers {

  def repUntil[T](p0: => Parser[T], q0: => Parser[Any] = ""): Parser[List[T]] = Parser { in =>
    lazy val p = p0
    lazy val q = q0
    val elems  = new ListBuffer[T]

    @tailrec def applyp(in0: Input): ParseResult[List[T]] = p(in0) match {
      case Success(x, rest) => elems += x; applyp(rest)
      case e: Error         => e // still have to propagate error
      case Failure(str, input) =>
        q(in0) match {
          case Success(_, next) => Success(elems.toList, next)
          case _: Failure       => Failure(str, input)
          case e: Error         => e // still have to propagate error
        }
      case _ => Success(elems.toList, in0)
    }

    applyp(in)
  }

  def rootItem: Parser[DSLRoot] = (ident <~ ":") ~ (ident <~ "{") ~ repUntil(entry, "}") ^^ {
    case key ~ tag ~ entries => DSLRoot(key, tag, entries)
  }

  def entry: Parser[Entry] = (attribute | metadata) <~ ";" | rootItem <~ ";".?

  def shorthandArgList[T]: Parser[List[Arg] ~ List[T]] = {
    val listParser = "[" ~> repUntil(arg <~ argsListSeparator, "]") ^^ { elems =>
      List(Arg.ListArg(elems))
    }
    listParser ~ success(List())
  }

  def metadata: Parser[Entry.Metadata] = "#" ~> ident ~ (allArgs | shorthandArgList) ^^ {
    case function ~ (args ~ kwargs) => Entry.Metadata(function, args, kwargs)
  }

  def attribute: Parser[Entry.Attribute] =
    (ident <~ ":") ~ fieldType ~ repUntil(annotation, guard("[;}]".r)) ^^ {
      case key ~ fieldType ~ annotations => Entry.Attribute(key, fieldType, annotations)
    }

  // find a comma or the end of the argument list
  def argsListSeparator: Parser[Unit] = ("," | guard("]" | ")" | "}")) ^^^ ()

  def arg: Parser[Arg] =
    ident ^^ Arg.TokenArg |
    wholeNumber ^^ (str => Arg.IntArg(str.toInt)) |
    floatingPointNumber ^^ (str => Arg.FloatingArg(str.toDouble))

  def kwarg: Parser[(String, Arg)] = ((ident <~ ":") ~ arg) ^^ { case ident ~ arg => (ident, arg) }

  def allArgs: Parser[List[Arg] ~ List[(String, Arg)]] =
    "(" ~> (rep(arg <~ argsListSeparator) ~ repUntil(kwarg <~ argsListSeparator, ")"))

  def fieldType: Parser[FieldType] = ident ~ allArgs.? ^^ {
    case name ~ Some(args ~ kwargs) => FieldType(name, args, kwargs)
    case name ~ None                => FieldType(name)
  }

  def annotation: Parser[Annotation] = "@" ~> ident ^^ Annotation
}

object DSLParser extends DSLParser {
  protected def printError(input: Input): String = {
    val lineNo = input.pos.line.toString
    lineNo + " | " + input.source.toString.split("\n")(input.pos.line - 1) + "\n" +
    (" " * (input.pos.column + 2 + lineNo.length)) + "^"
  }

  def parse(contents: InputStreamReader): Either[String, temple.DSL.DSLRoot] =
    parseAll(rootItem, contents) match {
      case Success(result, input) => Right(result)
      case NoSuccess(str, input)  => Left(str + '\n' + printError(input))
    }
}
