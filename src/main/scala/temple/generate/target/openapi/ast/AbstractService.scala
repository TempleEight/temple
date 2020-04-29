package temple.generate.target.openapi.ast

import temple.ast.AbstractAttribute
import temple.generate.CRUD.CRUD

sealed trait AbstractService {
  val name: String
  val operations: Iterable[CRUD]
  val attributes: Map[String, AbstractAttribute]
}

object AbstractService {

  case class Service(
    name: String,
    operations: Iterable[CRUD],
    attributes: Map[String, AbstractAttribute],
    structs: Iterable[Struct] = Nil,
  ) extends AbstractService

  case class Struct(
    name: String,
    operations: Iterable[CRUD],
    attributes: Map[String, AbstractAttribute],
  ) extends AbstractService
}
