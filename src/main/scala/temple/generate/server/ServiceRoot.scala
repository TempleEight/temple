package temple.generate.server

import temple.DSL.semantics.SemanticParsingException
import temple.ast.Attribute
import temple.ast.Metadata.{Database, Readable, Writable}
import temple.generate.CRUD.CRUD
import temple.utils.StringUtils

import scala.collection.immutable.ListMap

/**
  * ServiceRoot encapsulates all the information needed to generate a service
  *
  * @param name the service name to be generated
  * @param module the module name of the service to be generated // TODO: This is pretty Go specific, make it generic
  * @param comms a sequence of service names this service communicates with
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
  */
case class ServiceRoot(
  override val name: String,
  module: String,
  comms: Seq[String],
  opQueries: ListMap[CRUD, String],
  port: Int,
  idAttribute: IDAttribute,
  createdByAttribute: Option[CreatedByAttribute],
  attributes: ListMap[String, Attribute],
  datastore: Database,
  readable: Readable,
  writable: Writable,
  projectUsesAuth: Boolean,
  hasAuthBlock: Boolean,
) extends ServiceRoot.Name(name)

object ServiceRoot {

  class Name(val name: String) {
    if (!name.head.isUpper) throw new SemanticParsingException(s"ServiceRoot name ($name) must be capitalized")
    def decapitalizedName: String = StringUtils.decapitalize(name)
    def kebabName: String         = StringUtils.kebabCase(name)
  }
}
