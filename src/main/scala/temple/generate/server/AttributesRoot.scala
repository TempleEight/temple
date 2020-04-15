package temple.generate.server

import temple.ast.AbstractAttribute
import temple.ast.Metadata.{Database, Metrics, Readable, Writable}
import temple.generate.CRUD.CRUD

import scala.collection.immutable.{ListMap, SortedMap}

trait AttributesRoot extends ServiceName {
  override def name: String
  def opQueries: SortedMap[CRUD, String]
  def idAttribute: IDAttribute
  def createdByAttribute: Option[CreatedByAttribute]
  def attributes: ListMap[String, AbstractAttribute]
  def readable: Readable
  def writable: Writable

  def requestAttributes: ListMap[String, AbstractAttribute] = attributes.filter { case (_, attr) => attr.inRequest }

  def operations: Set[CRUD] = opQueries.keySet
}

object AttributesRoot {

  /**
    * ServiceRoot encapsulates all the information needed to generate a service
    *
    * @param name the service name to be generated
    * @param module the module name of the service to be generated // TODO: This is pretty Go specific, make it generic
    * @param comms a set of service names this service communicates with
    * @param opQueries a map of CRUD operations to their corresponding datastore query
    * @param port the port number this service will be served on
    * @param idAttribute the name of the ID field
    * @param createdByAttribute whether or not this service has a createdBy attribute. Also indicates whether this service has an auth block.
    * @param attributes the user-defined fields of the resource handled by this service
    * @param datastore the datastore being used
    * @param readable whether this service is readable by this or by all
    * @param writable whether this service is writable by this or by all
    * @param projectUsesAuth whether or not the project uses auth
    * @param hasAuthBlock whether or not this service has an auth block
    * @param metrics whether or not this service has metrics, and if so, which framework is used. TODO: Does not switch on framework, assumes Prometheus
    */
  case class ServiceRoot(
    name: String,
    module: String,
    comms: Set[ServiceName],
    opQueries: SortedMap[CRUD, String],
    port: Int,
    idAttribute: IDAttribute,
    createdByAttribute: Option[CreatedByAttribute],
    attributes: ListMap[String, AbstractAttribute],
    datastore: Database,
    readable: Readable,
    writable: Writable,
    projectUsesAuth: Boolean,
    hasAuthBlock: Boolean,
    metrics: Option[Metrics],
  ) extends ServiceName
      with AttributesRoot {}
}
