package temple.generate.target.openapi

import temple.ast.Attribute
import temple.generate.Endpoint

case class Service(
  name: String,
  endpoints: Set[Endpoint],
  attributes: Map[String, Attribute],
  structs: Map[String, Service.Struct] = Map.empty,
)

object Service {

  case class Struct(
    name: String,
    endpoints: Set[Endpoint],
    attributes: Map[String, Attribute],
  )
}
