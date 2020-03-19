package temple.generate.target.openapi.ast

import temple.ast.Attribute
import temple.generate.CRUD

case class Service(
  name: String,
  operations: Iterable[CRUD],
  attributes: Map[String, Attribute],
  structs: Map[String, Service.Struct] = Map.empty,
)

object Service {

  case class Struct(
    name: String,
    operations: Iterable[CRUD],
    attributes: Map[String, Attribute],
  )
}
