package temple.generate.server

import temple.ast.Attribute
import temple.generate.CRUD

case class ServiceRoot(
  name: String,
  module: String,
  comms: Seq[String],
  operations: Set[CRUD],
  port: Int,
  attributes: Map[String, Attribute],
)
