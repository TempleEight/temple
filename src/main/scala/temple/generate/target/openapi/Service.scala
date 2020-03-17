package temple.generate.target.openapi

import temple.DSL.semantics.Attribute
import temple.generate.CRUD

case class Service(
  name: String,
  operations: Set[CRUD],
  attributes: Map[String, Attribute],
  structs: Map[String, Service.Struct] = Map.empty,
)

object Service {

  case class Struct(
    name: String,
    operations: Set[CRUD],
    attributes: Map[String, Attribute],
  )
}
