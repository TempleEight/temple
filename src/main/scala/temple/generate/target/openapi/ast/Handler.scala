package temple.generate.target.openapi.ast

import io.circe.Json
import temple.generate.JsonEncodable

import scala.Option.when
import scala.collection.immutable.ListMap

// https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#operationObject
case class Handler(
  summary: String,
  description: String,
  tags: Seq[String],
  requestBody: Option[RequestBody],
  responses: Map[Int, Response],
) extends JsonEncodable.Partial {

  override def jsonOptionEntryIterator: IterableOnce[(String, Option[Json])] = Seq(
    "summary"     ~~> Some(summary),
    "description" ~~> when(description.nonEmpty) { description },
    "tags"        ~~> when(tags.nonEmpty) { tags },
    "requestBody" ~~> when(requestBody.nonEmpty) { requestBody },
    "responses"   ~~> when(responses.nonEmpty) { responses },
  )
}

object Handler {

  def apply(
    summary: String,
    description: String = "",
    tags: Seq[String] = Nil,
    requestBody: Option[RequestBody] = None,
    responses: Seq[(Int, Response)] = Seq(),
  ): Handler = Handler(summary, description, tags, requestBody, responses.to(ListMap))
}
