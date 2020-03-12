package temple.generate.target.openapi

// https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#operationObject
case class Path(
  summary: String,
  description: Option[String] = None,
  tags: Seq[String] = Nil,
  requestBody: Option[RequestBody] = None,
  responses: Map[Int, Response] = Map.empty,
)
