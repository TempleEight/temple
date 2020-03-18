package temple.generate.target.openapi.ast

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable

import scala.collection.immutable.ListMap
import scala.collection.mutable

case class Path(handlers: Map[HTTPVerb, Handler], parameters: Seq[Parameter] = Seq()) extends JsonEncodable.Object {

  override def jsonEntryIterator: IterableOnce[(String, Json)] =
    Option.when(parameters.nonEmpty) { "parameters" ~> parameters } ++ handlers.asJsonObject.toMap
}

object Path {

  case class Mutable(
    handlers: mutable.Map[HTTPVerb, Handler] = mutable.LinkedHashMap(),
    parameters: Seq[Parameter] = Seq(),
  ) {
    def toPath: Path = Path(handlers.to(ListMap), parameters)
  }
}
