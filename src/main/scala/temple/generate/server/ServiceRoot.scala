package temple.generate.server

import temple.ast.Attribute
import temple.generate.CRUD

import scala.collection.immutable.ListMap

case class ServiceRoot(
  name: String,
  module: String,
  comms: Seq[String],
  operations: Set[CRUD],
  port: Int,
  idAttribute: IDAttribute,
  createdByAttribute: Option[CreatedByAttribute],
  attributes: ListMap[String, Attribute],
  enumByCreatedBy: Boolean = false,
)
