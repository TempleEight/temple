package temple.DSL.parser

import temple.DSL.syntax._

import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/** A library of parser generators for the Templefile DSL */
class DSLParser extends JavaTokenParsers with UtilParsers {

  /** A parser generator for an entire Templefile */
  protected def templefile: Parser[Templefile] = repAll(rootItem)

  /** A parser generator for an identifier beginning in a lowercase letter */
  protected def lowerIdent: Parser[String] = guard("""[a-z]""".r) ~> ident

  /** A parser generator for an identifier beginning in an uppercase letter */
  protected def upperIdent: Parser[String] = guard("""[A-Z]""".r) ~> ident

  /** A parser generator for each item at the root level, i.e. a name, tag and block */
  protected def rootItem: Parser[DSLRootItem] = (upperIdent <~ ":") ~ (ident <~ "{") ~ repUntil(entry, "}") ^^ {
    case key ~ tag ~ entries => DSLRootItem(key, tag, entries)
  }

  /** A parser generator for a semicolon (optional at the end of a block). */
  protected def semicolon: Parser[String] = guard("""$""".r | "}") | ";"

  /** A parser generator for an entry within a block. */
  protected def entry: Parser[Entry] = rootItem <~ semicolon.? | (attribute | metadata) <~ semicolon

  /** A parser generator for a line of metadata */
  protected def metadata: Parser[Entry.Metadata] =
    "#" ~> lowerIdent ~ (allArgs | shorthandListArg | success(Args())) ^^ {
      case function ~ args => Entry.Metadata(function, args)
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
  protected def shorthandListArg[T]: Parser[Args] = listArg ^^ (list => Args(Seq(list)))

  /** A parser generator for a floating point number other than an integer
    *
    * This excludes integer literals, to avoid integers being misparsed as floating point numbers. */
  protected def floatingNumber: Parser[Double] =
    (
      """-?\d+(\.\d+)?([eE][+-]?\d+)""".r | // exponential
      """-?\d+\.\d+""".r                    // decimal
    ) ^^ (_.toDouble)

  /** A parser generator for an integer with an optional suffix (G, M or K) for 1e9, 1e6 or 1e3 respectively. */
  protected def scaledNumber: Parser[Int] = {
    val siPrefix = "[gG]".r ^^^ 1_000_000_000 | "[mM]".r ^^^ 1_000_000 | "[kK]".r ^^^ 1_000 | success(1)
    (wholeNumber ~ siPrefix) ^^ { case num ~ scale => num.toInt * scale }
  }

  /** A parser generator for any argument passed to a type or metadata */
  protected def arg: Parser[Arg] =
    ident ^^ Arg.TokenArg |
    floatingNumber ^^ Arg.FloatingArg |
    scaledNumber ^^ Arg.IntArg |
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
  protected def attributeType: Parser[AttributeType] = primitiveAttributeType | foreignAttributeType

  /** A parser generator for the type of a primitive attribute */
  protected def primitiveAttributeType: Parser[AttributeType.Primitive] = lowerIdent ~ allArgs.? ^^ {
    case name ~ maybeArgs => AttributeType.Primitive(name, maybeArgs.getOrElse(Args()))
  }

  /** A parser generator for the type of a foreign key attribute */
  protected def foreignAttributeType: Parser[AttributeType.Foreign] =
    // If there is an args list (so `not(allArgs)` fails), this parser will fail
    upperIdent <~ (not(allArgs) | failure("Foreign keys cannot have parameters")) ^^ AttributeType.Foreign

  /** A parser generator for an annotation on an attribute */
  protected def annotation: Parser[Annotation] = "@" ~> ident ^^ Annotation

  /** The updated whitespace parser, allowing for comments */
  // (\s+|//[^\n]*\n|/\*(\*(?!/)|[^\*])*\*/)+
  // (   |          |                      )+     any sequence of permutations of the following:
  //  \s+                                         - genuine whitespace
  //      //[^\n]*\n                              - a one-line comment, made of:
  //      //                                        - a double-slash
  //        [^\n]*                                  - zero or more characters other than newlines
  //              \n                                - a new line
  //                 /\*(\*(?!/)|[^\*])*\*/       - a multi-line comment, made of:
  //                 /\*                            - a slash-star
  //                    (       |     )*            - any number of characters that:
  //                     \*(?!/)                      - are a star not followed by a slash
  //                             [^\*]                - are not a star
  //                                    \*/         - a star-slash
  override protected val whiteSpace: Regex = """(\s+|//[^\n]*\n|/\*(\*(?!/)|[^\*])*\*/)+""".r
}
