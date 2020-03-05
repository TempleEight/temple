package temple.generate.service

import temple.generate.Endpoint
import temple.generate.FileSystem._

/** ServiceGenerator provides an interface for generating service boilerplate from an ADT */
trait ServiceGenerator {

  /** Given a ServiceRoot ADT, generate the service boilerplate in a specific language */
  def generate(serviceRoot: ServiceRoot): Map[File, FileContent]
}

object ServiceGenerator {

  /** Get the string representation of an endpoint, for use in the generated function name */
  private[service] def verb(endpoint: Endpoint): String = endpoint match {
    case Endpoint.ReadAll => "List"
    case Endpoint.Create  => "Create"
    case Endpoint.Read    => "Read"
    case Endpoint.Update  => "Update"
    case Endpoint.Delete  => "Delete"
  }
}
