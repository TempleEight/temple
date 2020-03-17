package temple.generate.service

import temple.generate.CRUD
import temple.generate.FileSystem._

/** ServiceGenerator provides an interface for generating service boilerplate from an ADT */
trait ServiceGenerator {

  /** Given a ServiceRoot ADT, generate the service boilerplate in a specific language */
  def generate(serviceRoot: ServiceRoot): Map[File, FileContent]
}

object ServiceGenerator {

  /** Get the string representation of an endpoint, for use in the generated function name */
  private[service] def verb(endpoint: CRUD): String = endpoint match {
    case CRUD.ReadAll => "List"
    case CRUD.Create  => "Create"
    case CRUD.Read    => "Read"
    case CRUD.Update  => "Update"
    case CRUD.Delete  => "Delete"
  }
}
