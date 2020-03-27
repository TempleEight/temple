package temple.DSL.syntax

import temple.DSL.syntax.Arg.ListArg
import temple.generate.utils.CodeTerm.{CodeWrap, mkCode}

/** Any element of a service/struct */
abstract class Entry(val typeName: String)

object Entry {

  case class Attribute(key: String, dataType: AttributeType, annotations: Seq[Annotation] = Nil)
      extends Entry("attribute") {
    override def toString: String = s"$key: $dataType${annotations.map(" " + _).mkString};"
  }

  case class Metadata(metaKey: String, args: Args = Args()) extends Entry("metadata") {

    private def argsToString: String = args match {
      case Args(Seq(list: ListArg), Seq()) => list.toTempleString
      case Args(Seq(), Seq())              => ""
      case args                            => CodeWrap.parens(args.toString)
    }
    override def toString: String = mkCode.stmt("#" + metaKey, argsToString)
  }
}
