package temple.DSL.parser

import temple.DSL._

import scala.util.parsing.combinator.JavaTokenParsers

/** A library of parser generators for the Templefile DSL */
class DSLParser extends JavaTokenParsers with UtilParsers {

  /** A parser generator for an entire Templefile */
  def templeFile: Parser[List[DSLRootItem]] = repAll(rootItem)

  /** A parser generator for each item at the root level, i.e. a name, tag and block */
  def rootItem: Parser[DSLRootItem] = (ident <~ ":") ~ (ident <~ "{") ~ repUntil(entry, "}") ^^ {
    case key ~ tag ~ entries => DSLRootItem(key, tag, entries)
  }

  /** A parser generator for an entry within a block. */
  def entry: Parser[Entry] = (attribute | metadata) <~ ";" | rootItem <~ ";".?

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

  /** A parser generator for a list of arguments in square brackets */
  def listArg: Parser[Arg] = "[" ~> repUntil(arg <~ argsListSeparator, "]") ^^ (elems => Arg.ListArg(elems))

  /** A parser generator for a list of arguments in square brackets, when used in shorthand to replace the brackets */
  def shorthandListArg[T]: Parser[List[Arg] ~ List[T]] = (listArg ^^ (list => List(list))) ~ success(List())

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
