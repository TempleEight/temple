package temple.DSL.parser

import temple.DSL.Syntax._
import scala.util.parsing.combinator.JavaTokenParsers

/** A library of parser generators for the Templefile DSL */
class DSLParser extends JavaTokenParsers with UtilParsers {

  /** A parser generator for an entire Templefile */
  protected def templefile: Parser[Templefile] = repAll(rootItem)

  /** A parser generator for each item at the root level, i.e. a name, tag and block */
  protected def rootItem: Parser[DSLRootItem] = (ident <~ ":") ~ (ident <~ "{") ~ repUntil(entry, "}") ^^ {
    case key ~ tag ~ entries => DSLRootItem(key, tag, entries)
  }

  /** A parser generator for an entry within a block. */
  protected def entry: Parser[Entry] = (attribute | metadata) <~ ";" | rootItem <~ ";".?

  /** A parser generator for a line of metadata */
  protected def metadata: Parser[Entry.Metadata] = "#" ~> ident ~ (allArgs | shorthandListArg) ^^ {
    case function ~ (args ~ kwargs) => Entry.Metadata(function, Args(args, kwargs))
  }

  /** A parser generator for a struct’s attribute */
  protected def attribute: Parser[Entry.Attribute] =
    (ident <~ ":") ~ attributeType ~ repUntil(annotation, guard("[;}]".r)) ^^ {
      case key ~ fieldType ~ annotations => Entry.Attribute(key, fieldType, annotations)
    }

  /** A parser generator for a comma or the end of the argument list */
  protected def argsListSeparator: Parser[Unit] = (guard("]" | ")" | "}") | ",") ^^^ ()

  /** A parser generator for a list of arguments in square brackets */
  protected def listArg: Parser[Arg] = "[" ~> repUntil(arg <~ argsListSeparator, "]") ^^ (elems => Arg.ListArg(elems))

  /** A parser generator for a list of arguments in square brackets, when used in shorthand to replace the brackets */
  protected def shorthandListArg[T]: Parser[Seq[Arg] ~ Seq[T]] = (listArg ^^ (list => Seq(list))) ~ success(Nil)

  /**
    * A parser generator for any argument passed to a type or metadata
    */
  protected def arg: Parser[Arg] =
    ident ^^ Arg.TokenArg |
    floatingPointNumber ^^ (str => Arg.FloatingArg(str.toDouble)) |
    wholeNumber ^^ (str => Arg.IntArg(str.toInt)) |
    listArg

  /** A parser generator for an argument keyed by a keyword */
  protected def kwarg: Parser[(String, Arg)] = ((ident <~ ":") ~ arg) ^^ { case ident ~ arg => (ident, arg) }

  /** A parser generator for a sequence of arguments, starting positionally and subsequently keyed.
    * If the parser fails after parsing the open bracket, commit is called to protect against being confused with nested
    * rootitems */
  protected def allArgs: Parser[Args] =
    "(" ~> commit(rep(arg <~ argsListSeparator) ~ repUntil(kwarg <~ argsListSeparator, ")")) ^^ {
      case posargs ~ kwargs => Args(posargs, kwargs)
    }

  /** A parser generator for the type of an attribute */
  protected def attributeType: Parser[AttributeType] = ident ~ allArgs.? ^^ {
    case name ~ maybeArgs => AttributeType(name, maybeArgs.getOrElse(Args()))
  }

  /** A parser generator for an annotation on an attribute */
  protected def annotation: Parser[Annotation] = "@" ~> ident ^^ Annotation
}
