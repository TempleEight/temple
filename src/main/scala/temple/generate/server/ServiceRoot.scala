package temple.generate.server

import temple.ast.Attribute
import temple.generate.CRUD

import scala.collection.immutable.ListMap

/**
  * ServiceRoot encapsulates all the information needed to generate a service
  *
  * @param name the service name to be generated
  * @param module the module name of the service to be generated // TODO: This is pretty Go specific, make it generic
  * @param comms a sequence of service names this service communicates with
  * @param operations a set of operations this service supports on the resource handled
  * @param port the port number this service will be served on
  * @param idAttribute the name and type of the ID field in this service
  * @param createdByAttribute the input name, name and type of the createdBy field in this service, and whether it is
  * used to enumerate the service in the List endpoint
  * @param attributes the user-defined fields of the resource handled by this service
  */
case class ServiceRoot(
  name: String,
  module: String,
  comms: Seq[String],
  operations: Set[CRUD],
  port: Int,
  idAttribute: IDAttribute,
  createdByAttribute: CreatedByAttribute,
  attributes: ListMap[String, Attribute],
)
