package temple.DSL

import temple.DSL

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.parsing.combinator._

class DSLParser extends JavaTokenParsers {

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

  def rootItem: Parser[DSLRoot] = (ident <~ ":") ~ (ident <~ "{") ~ repUntil(entry <~ ";", "}") ^^ {
    case key ~ tag ~ entries => DSLRoot(key, tag, entries)
  }

  def entry: Parser[Entry] = attribute

  def attribute: Parser[Entry.Attribute] = (ident <~ ":") ~ fieldType ~ repUntil(annotation, guard("[;}]".r)) ^^ {
    case key ~ fieldType ~ annotations => Entry.Attribute(key, fieldType, annotations)
  }

  def fieldType: Parser[FieldType] = ident ^^ FieldType.SimpleType

  def annotation: Parser[Annotation] = "@" ~> ident ^^ Annotation
}

object DSLParser extends DSLParser {

  def printError(input: Input): String = {
    val lineNo = input.pos.line.toString
    lineNo + " | " + input.source.toString.split("\n")(input.pos.line - 1) + "\n" +
    (" " * (input.pos.column + 2 + lineNo.length)) + "^"
  }

  def main(args: Array[String]): Unit = {
    val fSource = Source.fromFile(args(0))
    try parseAll(rootItem, fSource.reader()) match {
      case Success(result, input) => println(result)
      case NoSuccess(str, input)  => System.err.println(str + '\n' + printError(input))
    } finally {
      fSource.close()
    }
  }
}
