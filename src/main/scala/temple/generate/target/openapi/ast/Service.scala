package temple.generate.target.openapi.ast

import temple.ast.AbstractAttribute
import temple.generate.CRUD.CRUD

case class Service(
  name: String,
  operations: Iterable[CRUD],
  attributes: Map[String, AbstractAttribute],
  structs: Iterable[Service.Struct] = Nil,
)

object Service {

  case class Struct(
    name: String,
    operations: Iterable[CRUD],
    attributes: Map[String, AbstractAttribute],
  )
}
