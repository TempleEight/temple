package temple.generate.target.openapi

import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import temple.generate.JsonEncodable

import scala.Option.when

// https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#operationObject
case class Handler(
  summary: String,
  description: String = "",
  tags: Seq[String] = Nil,
  requestBody: Option[RequestBody] = None,
  responses: Map[Int, Response] = Map.empty,
  parameters: Seq[Parameter] = Seq(),
) extends JsonEncodable.Partial {

  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
    "summary"     -> Some(summary.asJson),
    "description" -> when(description.nonEmpty) { description.asJson },
    "tags"        -> when(tags.nonEmpty) { tags.asJson },
    "parameters"  -> when(parameters.nonEmpty) { parameters.asJson },
    "requestBody" -> when(requestBody.nonEmpty) { requestBody.asJson },
    "responses"   -> when(responses.nonEmpty) { responses.asJson },
  )
}
