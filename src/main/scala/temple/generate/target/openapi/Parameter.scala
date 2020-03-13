package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._
import temple.generate.JsonEncodable

import scala.Option.when

case class Parameter(
  in: Parameter.In,
  name: String,
  schema: OpenAPIType,
  required: Option[Boolean],
  description: String,
) extends JsonEncodable.Partial {

  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
    "in"          -> Some(in.asJson),
    "name"        -> Some(name.asJson),
    "schema"      -> Some(schema.asJson),
    "required"    -> required.map(_.asJson),
    "description" -> when(description.nonEmpty) { description.asJson },
  )
}

object Parameter {
  sealed trait In extends JsonEncodable

  case object InPath extends In { protected def toJson: Json = "path".asJson }
}
