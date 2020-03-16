package temple.generate.service

import temple.generate.Crud
import temple.generate.FileSystem._

/** ServiceGenerator provides an interface for generating service boilerplate from an ADT */
trait ServiceGenerator {

  /** Given a ServiceRoot ADT, generate the service boilerplate in a specific language */
  def generate(serviceRoot: ServiceRoot): Map[File, FileContent]
}

object ServiceGenerator {

  /** Get the string representation of an endpoint, for use in the generated function name */
  private[service] def verb(endpoint: Crud): String = endpoint match {
    case Crud.ReadAll => "List"
    case Crud.Create  => "Create"
    case Crud.Read    => "Read"
    case Crud.Update  => "Update"
    case Crud.Delete  => "Delete"
  }
}
