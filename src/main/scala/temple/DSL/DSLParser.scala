package temple.DSL

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.parsing.combinator._

class DSLParser extends JavaTokenParsers {
  def rootItem: Parser[DSLRoot] = (ident <~ ":") ~ (ident <~ "{") ~ (rep(entry <~ ";") <~ "}") ^^ {
    case key ~ tag ~ entries => DSLRoot(key, tag, entries)
  }

  def entry: Parser[Entry] = attribute

  def attribute: Parser[Entry.Attribute] = (ident <~ ":") ~ fieldType ~ rep(annotation) ^^ {
    case key ~ fieldType ~ annotations => Entry.Attribute(key, fieldType, annotations)
  }

  def fieldType: Parser[FieldType] = ident ^^ FieldType.SimpleType

  def annotation: Parser[Annotation] = "@" ~> ident ^^ Annotation
}

object DSLParser extends DSLParser {

  def main(args: Array[String]): Unit = {
    val fSource = Source.fromFile(args(0))
    try parseAll(rootItem, fSource.reader()) match {
      case Success(result, input) => println(result)
      case NoSuccess(str, input)  => System.err.println(str + '\n' + input)
    } finally {
      fSource.close()
    }
  }
}
