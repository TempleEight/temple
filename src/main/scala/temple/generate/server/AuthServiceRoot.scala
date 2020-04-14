package temple.generate.server

import temple.ast.Metadata.Metrics

/**
  * ServiceRoot encapsulates all the information needed to generate an auth service
  *
  * @param module the module name of the service to be generated // TODO: This is pretty Go specific, make it generic
  * @param port the port number this service will be served on
  * @param authAttribute the name and type of the field used to auth, e.g. email, username
  * @param idAttribute the name and type of the ID field in this service
  * @param createQuery the database query to create an auth
  * @param readQuery the database query to read an auth
  * @param metrics whether or not this auth service has metrics, and if so, which framework is used
  */
case class AuthServiceRoot(
  module: String,
  port: Int,
  authAttribute: AuthAttribute,
  idAttribute: IDAttribute,
  createQuery: String,
  readQuery: String,
  metrics: Option[Metrics],
)
